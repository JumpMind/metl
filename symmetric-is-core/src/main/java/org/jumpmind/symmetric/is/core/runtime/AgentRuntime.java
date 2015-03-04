package org.jumpmind.symmetric.is.core.runtime;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentStatus;
import org.jumpmind.symmetric.is.core.model.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.model.DeploymentStatus;
import org.jumpmind.symmetric.is.core.model.StartType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.FlowRuntime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.scheduling.support.CronTrigger;

public class AgentRuntime {

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

    ExecutorService componentFlowNodesExecutionThreads;

    ThreadPoolTaskScheduler componentFlowExecutionScheduler;

    ScheduledFuture<AgentWatchdogRunner> watchdogScheduled;

    public AgentRuntime(Agent agent, IConfigurationService configurationService,
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

            this.componentFlowNodesExecutionThreads = Executors
                    .newCachedThreadPool(new ThreadFactory() {
                        final AtomicInteger threadNumber = new AtomicInteger(1);
                        final String namePrefix = agent.getName().toLowerCase()
                                .replace(' ', '-').replace('_', '-');

                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r);
                            t.setName(namePrefix + "-node-" + threadNumber.getAndIncrement());
                            if (t.isDaemon()) {
                                t.setDaemon(false);
                            }
                            if (t.getPriority() != Thread.NORM_PRIORITY) {
                                t.setPriority(Thread.NORM_PRIORITY);
                            }
                            return t;
                        }
                    });

            this.componentFlowExecutionScheduler = new ThreadPoolTaskScheduler();
            this.componentFlowExecutionScheduler.setThreadNamePrefix(agent.getName()
                    .toLowerCase().replace(' ', '-').replace('_', '-')
                    + "-job-");
            /*
             * Threads are not pre-created. Set this plenty big so we don't run
             * out of threads
             */
            this.componentFlowExecutionScheduler.setPoolSize(100);
            this.componentFlowExecutionScheduler.initialize();

            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(
                    agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                deploy(deployment);
            }

            this.componentFlowExecutionScheduler.scheduleWithFixedDelay(new AgentWatchdogRunner(),
                    1000);

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

            watchdogScheduled.cancel(true);

            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(
                    agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                stop(deployment);
            }

            agent.setAgentStatus(AgentStatus.STOPPED);
            configurationService.save(agent);
            started = false;
            stopping = false;

            if (componentFlowExecutionScheduler != null) {
                this.componentFlowExecutionScheduler.destroy();
                this.componentFlowExecutionScheduler = null;
            }

            if (componentFlowNodesExecutionThreads != null) {
                this.componentFlowNodesExecutionThreads.shutdownNow();
                this.componentFlowNodesExecutionThreads = null;
            }

            log.info("Agent '{}' has been stopped", agent);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public AgentDeployment deploy(ComponentFlowVersion componentFlowVersion) {
        AgentDeployment deployment = agent.getAgentDeploymentFor(componentFlowVersion);
        if (deployment == null) {
            deployment = new AgentDeployment(componentFlowVersion);
            deployment.setAgentId(agent.getId());
            deployment.setComponentFlowVersionId(componentFlowVersion.getId());
            configurationService.save(deployment);

            List<AgentDeployment> deployments = agent.getAgentDeployments();
            deployments.remove(deployment);
            deployments.add(deployment);

            deploy(deployment);
        }
        return deployment;
    }

    private void deploy(final AgentDeployment deployment) {
        try {

            log.info("Deploying '{}' to '{}'", deployment.getComponentFlowVersion().toString(),
                    agent.getName());
            FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory,
                    connectionFactory, new ExecutionTracker(deployment),
                    componentFlowNodesExecutionThreads);
            coordinators.put(deployment, flowRuntime);

            if (deployment.getComponentFlowVersion().asStartType() == StartType.ON_DEPLOY) {
                flowRuntime.start();
                deployment.setStatus(DeploymentStatus.RUNNING.name());
            } else {
                if (deployment.getComponentFlowVersion().asStartType() == StartType.SCHEDULED_CRON) {
                    String cron = deployment.getComponentFlowVersion().getStartExpression();
                    log.info(
                            "Scheduling '{}' on '{}' with a cron expression of '{}'  The next run time should be at: {}",
                            new Object[] { deployment.getComponentFlowVersion().toString(),
                                    agent.getName(), cron,
                                    new CronSequenceGenerator(cron).next(new Date()) });

                    ScheduledFuture<?> future = this.componentFlowExecutionScheduler.schedule(
                            new FlowRunner(flowRuntime), new CronTrigger(cron));
                    scheduled.put(deployment, future);
                }

                deployment.setStatus(DeploymentStatus.STOPPED.name());
            }

            deployment.setMessage("");
            log.info("Flow '{}' has been deployed", deployment.getComponentFlowVersion().getName());
        } catch (Exception e) {
            log.warn("Failed to start '{}'", deployment.getComponentFlowVersion().getName(), e);
            deployment.setStatus(DeploymentStatus.ERROR.name());
            deployment.setMessage(ExceptionUtils.getRootCauseMessage(e));
        }
        configurationService.save(deployment);
    }

    public boolean scheduleNow(AgentDeployment deployment) {
        ScheduledFuture<?> future = scheduled.get(deployment);
        if (future == null || future.isDone()) {
            log.info("Scheduling '{}' on '{}' for now", new Object[] {
                    deployment.getComponentFlowVersion().toString(), agent.getName() });

            FlowRuntime flowRuntime = coordinators.get(deployment);
            future = this.componentFlowExecutionScheduler.schedule(new FlowRunner(flowRuntime),
                    new Date());
            scheduled.put(deployment, future);
            return true;
        } else {
            return false;
        }
    }

    protected void stop(AgentDeployment deployment) {
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
            } catch (Exception e) {
                log.warn("Failed to stop '{}'", deployment.getComponentFlowVersion().getName(), e);
            }
        }
    }

    public void undeploy(AgentDeployment deployment) {
        stop(deployment);
        configurationService.delete(deployment);
    }

    protected FlowRuntime getComponentFlowCoordinator(AgentDeployment deployment) {
        return coordinators.get(deployment);
    }

    class FlowRunner implements Runnable {

        FlowRuntime flowRuntime;

        public FlowRunner(FlowRuntime flowRuntime) {
            this.flowRuntime = flowRuntime;
        }

        @Override
        public void run() {
            try {
                AgentDeployment deployment = flowRuntime.getDeployment();
                log.info("Scheduled '{}' on '{}' is running", deployment.getComponentFlowVersion()
                        .toString(), agent.getName());
                flowRuntime.start();
                flowRuntime.waitForFlowCompletion();
                log.info("Scheduled '{}' on '{}' is finished", deployment.getComponentFlowVersion()
                        .toString(), agent.getName());
            } catch (Exception e) {
                log.error("Error while waiting for the flow to complete", e);
            }
        }
    }

    class AgentWatchdogRunner implements Runnable {
        @Override
        public void run() {
        }
    }

}
