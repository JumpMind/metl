package org.jumpmind.symmetric.is.core.runtime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentParameter;
import org.jumpmind.symmetric.is.core.model.AgentStatus;
import org.jumpmind.symmetric.is.core.model.DeploymentStatus;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowParameter;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.StartType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.persist.IExecutionService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.AsyncRecorder;
import org.jumpmind.symmetric.is.core.runtime.flow.FlowRuntime;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
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

    Map<String, IResource> resources = new HashMap<String, IResource>();

    IConfigurationService configurationService;

    IComponentFactory componentFactory;

    IExecutionService executionService;

    IResourceFactory resourceFactory;

    ExecutorService flowStepsExecutionThreads;

    ThreadPoolTaskScheduler flowExecutionScheduler;

    ScheduledFuture<AgentWatchdogRunner> watchdogScheduled;

    AsyncRecorder recorder;

    public AgentRuntime(Agent agent, IConfigurationService configurationService,
            IExecutionService executionService, IComponentFactory componentFactory,
            IResourceFactory resourceFactory) {
        this.agent = agent;
        this.executionService = executionService;
        this.configurationService = configurationService;
        this.componentFactory = componentFactory;
        this.resourceFactory = resourceFactory;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public synchronized void start() {
        if (!started && !starting) {
            starting = true;
            log.info("Agent '{}' is being started", agent);

            this.flowStepsExecutionThreads = Executors.newCachedThreadPool(new ThreadFactory() {
                final AtomicInteger threadNumber = new AtomicInteger(1);
                final String namePrefix = agent.getName().toLowerCase().replace(' ', '-')
                        .replace('_', '-');

                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r);
                    t.setName(namePrefix + "-step-" + threadNumber.getAndIncrement());
                    if (t.isDaemon()) {
                        t.setDaemon(false);
                    }
                    if (t.getPriority() != Thread.NORM_PRIORITY) {
                        t.setPriority(Thread.NORM_PRIORITY);
                    }
                    return t;
                }
            });

            this.flowExecutionScheduler = new ThreadPoolTaskScheduler();
            this.flowExecutionScheduler.setThreadNamePrefix(agent.getName().toLowerCase()
                    .replace(' ', '-').replace('_', '-')
                    + "-job-");
            /*
             * Threads are not pre-created. Set this plenty big so we don't run
             * out of threads
             */
            this.flowExecutionScheduler.setPoolSize(100);
            this.flowExecutionScheduler.initialize();

            this.recorder = new AsyncRecorder(executionService);
            this.flowStepsExecutionThreads.execute(this.recorder);

            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(
                    agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                deploy(deployment);
            }

            this.flowExecutionScheduler.scheduleWithFixedDelay(new AgentWatchdogRunner(), 1000);

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

            this.recorder.shutdown();

            agent.setAgentStatus(AgentStatus.STOPPED);
            configurationService.save(agent);

            started = false;
            stopping = false;

            if (flowExecutionScheduler != null) {
                this.flowExecutionScheduler.destroy();
                this.flowExecutionScheduler = null;
            }

            if (flowStepsExecutionThreads != null) {
                this.flowStepsExecutionThreads.shutdownNow();
                this.flowStepsExecutionThreads = null;
            }
            
            Collection<IResource> resourceCollection = resources.values();
                for (IResource resource : resourceCollection) {
                    log.info("Stopping the {} resource on the {} agent", resource.getResource().getName(), agent.getName());
                    resource.stop();
                }

            log.info("Agent '{}' has been stopped", agent);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public AgentDeployment deploy(Flow flow, Map<String, String> parameters) {
        AgentDeployment deployment = agent.getAgentDeploymentFor(flow);
        if (deployment == null) {
            deployment = new AgentDeployment(flow);
            deployment.setAgentId(agent.getId());
            deployment.setFlow(flow);

            List<FlowParameter> defaultParameters = flow.getFlowParameters();
            for (FlowParameter flowParameter : defaultParameters) {
                if (!parameters.containsKey(flowParameter.getName())) {
                    parameters.put(flowParameter.getName(), flowParameter.getDefaultValue());
                }
            }

            Set<String> paramKeys = parameters.keySet();
            for (String paramKey : paramKeys) {
                deployment.getAgentDeploymentParameters().add(
                        new AgentDeploymentParameter(paramKey, parameters.get(paramKey), deployment
                                .getId()));
            }
            configurationService.save(deployment);

            List<AgentDeployment> deployments = agent.getAgentDeployments();
            deployments.remove(deployment);
            deployments.add(deployment);

            deploy(deployment);
        }
        return deployment;
    }

    protected void deployResources(Flow flow) {
        Set<Resource> flowResources = flow.findResources();
        for (Resource flowResource : flowResources) {
            IResource alreadyDeployed = resources.get(flowResource.getId());

            Map<String, SettingDefinition> settings = resourceFactory
                    .getSettingDefinitionsForResourceType(flowResource.getType());
            TypedProperties defaultSettings = flowResource.toTypedProperties(settings);
            TypedProperties overrideSettings = agent.toTypedProperties(flowResource);
            TypedProperties combined = new TypedProperties(defaultSettings);
            combined.putAll(overrideSettings);

            boolean redeploy = true;
            if (alreadyDeployed != null) {
                redeploy = false;
                Resource deployedResource = alreadyDeployed.getResource();
                TypedProperties alreadyDeployedOverrides = alreadyDeployed.getAgentOverrides();
                TypedProperties alreadyDeployedDefaultSettings = deployedResource
                        .toTypedProperties(settings);
                TypedProperties alreadyDeployedCombined = new TypedProperties(
                        alreadyDeployedDefaultSettings);
                alreadyDeployedCombined.putAll(alreadyDeployedOverrides);

                for (Object key : combined.keySet()) {
                    Object newObj = combined.get(key);
                    Object oldObj = alreadyDeployedCombined.get(key);
                    if (!ObjectUtils.equals(newObj, oldObj)) {
                        redeploy = true;
                        break;
                    }
                }

                if (redeploy) {
                    log.info("Undeploying the {} resource to the {} agent", flowResource.getName(), agent.getName());
                    alreadyDeployed.stop();
                }

            }

            if (redeploy) {
                log.info("Deploying the {} resource to the {} agent", flowResource.getName(), agent.getName());
                IResource resource = resourceFactory.create(flowResource, overrideSettings);
                resources.put(flowResource.getId(), resource);
            }
        }

    }

    private void deploy(final AgentDeployment deployment) {
        try {
            log.info("Deploying '{}' to '{}'", deployment.getFlow().toString(), agent.getName());

            configurationService.refresh(deployment.getFlow());
            
            deployResources(deployment.getFlow());

            FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory,
                    resourceFactory, new ExecutionTrackerRecorder(agent, deployment, recorder),
                    flowStepsExecutionThreads);
            coordinators.put(deployment, flowRuntime);

            if (deployment.asStartType() == StartType.ON_DEPLOY) {
                scheduleNow(deployment);
            } else {
                if (deployment.asStartType() == StartType.SCHEDULED_CRON) {
                    String cron = deployment.getStartExpression();
                    log.info(
                            "Scheduling '{}' on '{}' with a cron expression of '{}'  The next run time should be at: {}",
                            new Object[] { deployment.getFlow().toString(), agent.getName(), cron,
                                    new CronSequenceGenerator(cron).next(new Date()) });

                    ScheduledFuture<?> future = this.flowExecutionScheduler.schedule(
                            new FlowRunner(null, flowRuntime), new CronTrigger(cron));
                    scheduled.put(deployment, future);
                }

                deployment.setStatus(DeploymentStatus.STOPPED.name());
            }

            deployment.setMessage("");
            log.info("Flow '{}' has been deployed", deployment.getFlow().getName());
        } catch (Exception e) {
            log.warn("Failed to start '{}'", deployment.getFlow().getName(), e);
            deployment.setStatus(DeploymentStatus.ERROR.name());
            deployment.setMessage(ExceptionUtils.getRootCauseMessage(e));
        }
        configurationService.save(deployment);
    }

    public String scheduleNow(AgentDeployment deployment) {
        ScheduledFuture<?> future = scheduled.get(deployment);
        if (future == null || future.isDone()) {
            log.info("Scheduling '{}' on '{}' for now", new Object[] {
                    deployment.getFlow().toString(), agent.getName() });

            FlowRuntime flowRuntime = coordinators.get(deployment);
            String executionId = UUID.randomUUID().toString();
            future = this.flowExecutionScheduler.schedule(new FlowRunner(executionId, flowRuntime),
                    new Date());
            scheduled.put(deployment, future);
            return executionId;
        } else {
            return null;
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
                log.info("Flow '{}' has been undeployed", deployment.getFlow().getName());
            } catch (Exception e) {
                log.warn("Failed to stop '{}'", deployment.getFlow().getName(), e);
            }
        }
    }

    public void undeploy(AgentDeployment deployment) {
        stop(deployment);
        configurationService.delete(deployment);
    }

    protected FlowRuntime getFlowCoordinator(AgentDeployment deployment) {
        return coordinators.get(deployment);
    }

    class FlowRunner implements Runnable {

        FlowRuntime flowRuntime;

        String executionId;

        public FlowRunner(String executionId, FlowRuntime flowRuntime) {
            this.flowRuntime = flowRuntime;
            this.executionId = executionId;
        }

        @Override
        public void run() {
            AgentDeployment deployment = flowRuntime.getDeployment();
            try {
                log.info("Scheduled '{}' on '{}' is running", deployment.getFlow().toString(),
                        agent.getName());
                flowRuntime.start(executionId, resources);
            } catch (Exception e) {
                log.error("Error while waiting for the flow to complete", e);
                flowRuntime.stop();
            } finally {
                flowRuntime.waitForFlowCompletion();

                flowRuntime.notifyStepsTheFlowIsComplete();

                log.info("Scheduled '{}' on '{}' is finished", deployment.getFlow().toString(),
                        agent.getName());
            }
        }
    }

    class AgentWatchdogRunner implements Runnable {
        @Override
        public void run() {
        }
    }

}
