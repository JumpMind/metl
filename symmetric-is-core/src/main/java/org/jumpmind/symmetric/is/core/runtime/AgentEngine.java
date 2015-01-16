package org.jumpmind.symmetric.is.core.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.AgentStatus;
import org.jumpmind.symmetric.is.core.config.DeploymentStatus;
import org.jumpmind.symmetric.is.core.config.StartType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.FlowRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

public class AgentEngine {

    final Logger log = LoggerFactory.getLogger(getClass());

    Agent agent;

    boolean started = false;

    boolean starting = false;

    boolean stopping = false;

    Map<AgentDeployment, FlowRuntime> coordinators = new HashMap<AgentDeployment, FlowRuntime>();
    
    Map<AgentDeployment, ScheduledFuture<?>> scheduled = new HashMap<AgentDeployment, ScheduledFuture<?>>();

    IConfigurationService configurationService;

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    ExecutorService executorService;

    ThreadPoolTaskScheduler taskScheduler;

    public AgentEngine(Agent agent, IConfigurationService configurationService,
            IComponentFactory componentFactory, IConnectionFactory connectionFactory) {
        this.agent = agent;
        this.configurationService = configurationService;
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;

    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public synchronized void start() {
        if (!started && !starting) {
            starting = true;
            log.info("Agent '{}' is being started", agent);
            

            executorService = Executors.newCachedThreadPool(new ThreadFactory() {
                final AtomicInteger threadNumber = new AtomicInteger(1);
                final String namePrefix = agent.getData().getName().toLowerCase().replace(' ', '-')
                        .replace('_', '-');

                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName(namePrefix + threadNumber.getAndIncrement());
                    if (t.isDaemon()) {
                        t.setDaemon(false);
                    }
                    if (t.getPriority() != Thread.NORM_PRIORITY) {
                        t.setPriority(Thread.NORM_PRIORITY);
                    }
                    return t;
                }
            });

            this.taskScheduler = new ThreadPoolTaskScheduler();
            this.taskScheduler.setThreadNamePrefix(agent.getData().getName().toLowerCase()
                    .replace(' ', '-').replace('_', '-'));
            this.taskScheduler.setPoolSize(10);
            this.taskScheduler.initialize();
            
            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(
                    agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                deploy(deployment);
            }
            agent.setAgentStatus(AgentStatus.RUNNING);
            configurationService.save(agent);

            started = true;
            starting = false;
            log.info("Agent '{}' has been started", agent);
        }
    }

    public synchronized void stop() {
        if (started && !stopping) {
            stopping = true;
            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(
                    agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                undeploy(deployment);
            }

            agent.setAgentStatus(AgentStatus.STOPPED);
            configurationService.save(agent);
            started = false;
            stopping = false;

            if (taskScheduler != null) {
                this.taskScheduler.destroy();
                this.taskScheduler = null;
            }
            
            if (executorService != null) {
                this.executorService.shutdownNow();
                this.executorService = null;
            }
            
            log.info("Agent '{}' has been stopped", agent);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void deploy(AgentDeployment deployment) {
        try {
            List<AgentDeployment> deployments = agent.getAgentDeployments();
            deployments.remove(deployment);
            deployments.add(deployment);
            
            log.info("Deploying '{}' to '{}'", deployment.getComponentFlowVersion().toString(), agent.getData().getName());
            final FlowRuntime runtime = new FlowRuntime(deployment, componentFactory,
                    connectionFactory, new ExecutionTracker(deployment), executorService);
            coordinators.put(deployment, runtime);

            if (deployment.getComponentFlowVersion().getStartType() == StartType.ON_DEPLOY) {
                runtime.start();
                deployment.getData().setStatus(DeploymentStatus.RUNNING.name());
            } else {
                if (deployment.getComponentFlowVersion().getStartType() == StartType.SCHEDULED_CRON) {
                    String cron = deployment.getComponentFlowVersion().getStartExpression();
                    log.info("Scheduling with a cron expression of '{}' for '{}'", new Object[] {
                            cron, deployment.getComponentFlowVersion().toString() });

                    ScheduledFuture<?> future = this.taskScheduler.schedule(new Runnable() {

                        @Override
                        public void run() {
                            try {
                                runtime.start();
                                runtime.waitForFlowCompletion();
                            } catch (Exception e) {
                                log.error("Error while waiting for the flow to complete", e);
                            }
                        }
                    }, new CronTrigger(cron));
                    
                    scheduled.put(deployment, future);
                }

                deployment.getData().setStatus(DeploymentStatus.STOPPED.name());
            }

            deployment.getData().setMessage("");
            log.info("Flow '{}' has been deployed", deployment.getComponentFlowVersion().getName());
        } catch (Exception e) {
            log.warn("Failed to start '{}'", deployment.getComponentFlowVersion().getName(), e);
            deployment.getData().setStatus(DeploymentStatus.ERROR.name());
            deployment.getData().setMessage(ExceptionUtils.getRootCauseMessage(e));
        }
        configurationService.save(deployment.getData());
    }

    public void undeploy(AgentDeployment deployment) {
        agent.getAgentDeployments().remove(deployment);
        
        ScheduledFuture<?> future = scheduled.get(deployment);
        if (future != null) {
            future.cancel(true);
            scheduled.remove(future);
        }
        
        FlowRuntime coordinator = coordinators.get(deployment);
        if (coordinator != null) {
            try {
                coordinator.stop();
                log.info("Flow '{}' has been undeployed", deployment.getComponentFlowVersion()
                        .getName());
                deployment.getData().setStatus(DeploymentStatus.STOPPED.name());
                deployment.getData().setMessage("");
                configurationService.save(deployment.getData());
            } catch (Exception e) {
                log.warn("Failed to stop '{}'", deployment.getComponentFlowVersion().getName(), e);
            }
        }
                
    }

    protected FlowRuntime getComponentFlowCoordinator(AgentDeployment deployment) {
        return coordinators.get(deployment);
    }

}
