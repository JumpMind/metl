/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.AgentDeploymentParameter;
import org.jumpmind.metl.core.model.AgentStatus;
import org.jumpmind.metl.core.model.DeploymentStatus;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.model.StartType;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.flow.AsyncRecorder;
import org.jumpmind.metl.core.runtime.flow.FlowRuntime;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.metl.core.util.ThreadUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
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

    Map<AgentDeployment, FlowRuntime> deployedFlows = new HashMap<AgentDeployment, FlowRuntime>();

    Map<AgentDeployment, ScheduledFuture<?>> scheduledDeployments = new HashMap<AgentDeployment, ScheduledFuture<?>>();

    Map<String, IResourceRuntime> deployedResources = new HashMap<String, IResourceRuntime>();

    Map<String, String> globalSettings;
    
    IConfigurationService configurationService;

    IComponentRuntimeFactory componentFactory;

    IExecutionService executionService;

    IResourceFactory resourceFactory;

    ExecutorService flowStepsExecutionThreads;

    ThreadPoolTaskScheduler flowExecutionScheduler;

    ScheduledFuture<?> agentRequestHandler;

    AsyncRecorder recorder;

    public AgentRuntime(Agent agent, IConfigurationService configurationService,
            IExecutionService executionService, IComponentRuntimeFactory componentFactory,
            IResourceFactory resourceFactory) {
        this.agent = agent;
        this.executionService = executionService;
        this.configurationService = configurationService;
        this.componentFactory = componentFactory;
        this.resourceFactory = resourceFactory;
    }
    
    public boolean cancel(String executionId) {
        boolean cancelled = false;
        for (FlowRuntime flowRuntime : new HashSet<FlowRuntime>(deployedFlows.values())) {
            if (executionId.equals(flowRuntime.getExecutionId())) {
                if (flowRuntime.isRunning()) {
                    flowRuntime.cancel();
                    cancelled = true;
                }
            }
        }
        return cancelled;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public synchronized void start() {
        if (!started && !starting) {
            starting = true;
            log.info("Agent '{}' is being started", agent);
            
            executionService.markAbandoned(agent.getId());

            String agentName = agent.getName().toLowerCase();
            if (agentName.startsWith("<")) {
                agentName = agentName.substring(1);
            }
            if (agentName.endsWith(">")) {
                agentName = agentName.substring(0, agentName.length()-1);
            }
            if (agentName.indexOf(".") > 0) {
                agentName = agentName.substring(0, agentName.indexOf("."));
            }
            final String namePrefix = LogUtils.normalizeName(agentName);

            this.flowStepsExecutionThreads = ThreadUtils.createUnboundedThreadPool(namePrefix);

            this.flowExecutionScheduler = new ThreadPoolTaskScheduler();
            this.flowExecutionScheduler.setDaemon(true);
            this.flowExecutionScheduler.setThreadNamePrefix(namePrefix
                    + "-job-");
            /*
             * Threads are not pre-created. Set this plenty big so we don't run
             * out of threads
             */
            this.flowExecutionScheduler.setPoolSize(100);
            this.flowExecutionScheduler.initialize();

            this.globalSettings = configurationService.findGlobalSettingsAsMap();
            this.recorder = new AsyncRecorder(executionService);
            this.flowStepsExecutionThreads.execute(this.recorder);

            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(
                    agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                deploy(deployment);
            }

            agentRequestHandler = this.flowExecutionScheduler.scheduleWithFixedDelay(new AgentRequestHandler(), 10000);

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

            agentRequestHandler.cancel(true);

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
            
            Collection<IResourceRuntime> resourceCollection = deployedResources.values();
                for (IResourceRuntime resource : resourceCollection) {
                    log.info("Stopping the {} resource on the {} agent", resource.getResource().getName(), agent.getName());
                    resource.stop();
                }

            log.info("Agent '{}' has been stopped", agent);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public synchronized AgentDeployment deploy(Flow flow, Map<String, String> parameters) {
        AgentDeployment deployment = agent.getAgentDeploymentFor(flow);
        if (deployment == null) {
            deployment = new AgentDeployment(flow);
            deployment.setStatus(DeploymentStatus.REQUEST_DEPLOY.name());
            deployment.setAgentId(agent.getId());
            deployment.setFlow(flow);

            List<FlowParameter> defaultParameters = flow.getFlowParameters();
            for (FlowParameter flowParameter : defaultParameters) {
                String value = flowParameter.getDefaultValue();
                if (parameters.containsKey(flowParameter.getName())) {
                    value = parameters.get(flowParameter.getName());
                }
                deployment.getAgentDeploymentParameters().add(
                        new AgentDeploymentParameter(flowParameter.getName(), value, deployment
                                .getId(), flowParameter.getId()));

            }

            agent.getAgentDeployments().remove(deployment);
            agent.getAgentDeployments().add(deployment);
            configurationService.save(deployment);

            List<AgentDeployment> deployments = agent.getAgentDeployments();
            deployments.remove(deployment);
            deployments.add(deployment);

            deploy(deployment);
        }
        return deployment;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected void deployResources(Flow flow) {
        List<ResourceName> flowResourceNames = 
        configurationService.findResourcesInProject(flow.getProjectVersionId());
        for (ResourceName flowResourceName : flowResourceNames) {
            Resource flowResource = configurationService.findResource(flowResourceName.getId());
            IResourceRuntime alreadyDeployed = deployedResources.get(flowResource.getId());
    
            Map<String, SettingDefinition> settings = resourceFactory
                    .getSettingDefinitionsForResourceType(flowResource.getType());
            TypedProperties defaultSettings = flowResource.toTypedProperties(settings);
            TypedProperties overrideSettings = agent.toTypedProperties(flowResource);
            TypedProperties combined = new TypedProperties(defaultSettings);
            combined.putAll(overrideSettings);
            Set<Entry<Object, Object>> entries = combined.entrySet();
            for (Entry<Object, Object> entry : entries) {
                String value = (String)entry.getValue();
                if (value != null) {
                    value = FormatUtils.replaceTokens(value, (Map)System.getProperties(), true);
                    entry.setValue(value);
                }
            }
    
            boolean deploy = true;
            if (alreadyDeployed != null) {
                deploy = false;
                Resource deployedResource = alreadyDeployed.getResource();
                TypedProperties alreadyDeployedOverrides = alreadyDeployed.getResourceRuntimeSettings();
                
                // TODO the runtime is already combined.  change this
                TypedProperties alreadyDeployedDefaultSettings = deployedResource
                        .toTypedProperties(settings);
                TypedProperties alreadyDeployedCombined = new TypedProperties(
                        alreadyDeployedDefaultSettings);
                alreadyDeployedCombined.putAll(alreadyDeployedOverrides);
    
                for (Object key : combined.keySet()) {
                    Object newObj = combined.get(key);
                    Object oldObj = alreadyDeployedCombined.get(key);
                    if (!ObjectUtils.equals(newObj, oldObj)) {
                        deploy = true;
                        break;
                    }
                }
    
                if (deploy) {
                    log.info("Undeploying the {} resource to the {} agent", flowResource.getName(), agent.getName());
                    alreadyDeployed.stop();
                }
            }
    
            if (deploy) {
                log.info("Deploying the {} resource to the {} agent", flowResource.getName(), agent.getName());
                IResourceRuntime resource = resourceFactory.create(flowResource, combined);
                deployedResources.put(flowResource.getId(), resource);
            }
        }
    }

    private void deploy(final AgentDeployment deployment) {
        DeploymentStatus status = deployment.getDeploymentStatus();
        if (!status.equals(DeploymentStatus.DISABLED) && !status.equals(DeploymentStatus.REQUEST_DISABLE) &&
                !status.equals(DeploymentStatus.REQUEST_UNDEPLOY)) {
            try {
                log.info("Deploying '{}' to '{}'", deployment.getFlow().toString(), agent.getName());
    
                deployResources(deployment.getFlow());
    
                FlowRuntime flowRuntime = new FlowRuntime(deployment, componentFactory,
                        resourceFactory, new ExecutionTrackerRecorder(agent, deployment, recorder),
                        flowStepsExecutionThreads, configurationService);
                deployedFlows.put(deployment, flowRuntime);
    
                if (deployment.asStartType() == StartType.ON_DEPLOY) {
                    scheduleNow(deployment);
                } else if (deployment.asStartType() == StartType.SCHEDULED_CRON) {
                    String cron = deployment.getStartExpression();
                    log.info(
                            "Scheduling '{}' on '{}' with a cron expression of '{}'  The next run time should be at: {}",
                            new Object[] { deployment.getFlow().toString(), agent.getName(), cron,
                                    new CronSequenceGenerator(cron).next(new Date()) });
    
                    ScheduledFuture<?> future = this.flowExecutionScheduler.schedule(
                            new FlowRunner(UUID.randomUUID().toString(), flowRuntime), new CronTrigger(cron));
                    scheduledDeployments.put(deployment, future);
                }
    
                deployment.setStatus(DeploymentStatus.DEPLOYED.name());
                deployment.setMessage("");
                log.info("Flow '{}' has been deployed", deployment.getFlow().getName());
            } catch (Exception e) {
                log.warn("Failed to start '{}'", deployment.getFlow().getName(), e);
                deployment.setStatus(DeploymentStatus.ERROR.name());
                deployment.setMessage(ExceptionUtils.getRootCauseMessage(e));
            }
            configurationService.save(deployment);
        }
    }    

    public String scheduleNow(AgentDeployment deployment) {
        ScheduledFuture<?> future = scheduledDeployments.get(deployment);
        if (future == null || future.isDone()) {
            log.info("Scheduling '{}' on '{}' for now", new Object[] {
                    deployment.getName(), agent.getName() });

            FlowRuntime flowRuntime = deployedFlows.get(deployment);
            String executionId = UUID.randomUUID().toString();
            future = this.flowExecutionScheduler.schedule(new FlowRunner(executionId, flowRuntime),
                    new Date());
            scheduledDeployments.put(deployment, future);
            return executionId;
        } else {
            log.info("Returning reference to currently running deployment '{}' on agent '{}'", deployment.getName(), agent.getName());
            return deployedFlows.get(deployment).getExecutionId();
        }
    }

    protected void stop(AgentDeployment deployment) {
        ScheduledFuture<?> future = scheduledDeployments.get(deployment);
        if (future != null) {
            future.cancel(true);
            scheduledDeployments.remove(future);
        }

        FlowRuntime flowRuntime = deployedFlows.get(deployment);
        if (flowRuntime != null) {
            try {
                flowRuntime.cancel();
                log.info("Flow '{}' has been undeployed", deployment.getFlow().getName());
            } catch (Exception e) {
                log.warn("Failed to stop '{}'", deployment.getFlow().getName(), e);
            }
        }
    }
    
    public Collection<IResourceRuntime> getDeployedResources() {
        return new HashSet<IResourceRuntime>(deployedResources.values());
    }

    public synchronized void undeploy(AgentDeployment deployment) {
        stop(deployment);
        configurationService.delete(deployment);
        agent.getAgentDeployments().remove(deployment);
    }

    protected FlowRuntime getFlowCoordinator(AgentDeployment deployment) {
        return deployedFlows.get(deployment);
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
            if (isBlank(executionId)) {
                executionId = UUID.randomUUID().toString();
            }
            AgentDeployment deployment = flowRuntime.getDeployment();
            try {
                log.info("Scheduled deployment '{}' is running on the '{}' agent", deployment.getName(),
                        agent.getName());
                configurationService.refresh(deployment.getFlow());
                List<Notification> notifications = configurationService.findNotificationsForDeployment(deployment);
                flowRuntime.start(executionId, deployedResources, agent, notifications, globalSettings);
            } catch (Exception e) {
                log.error("Error while waiting for the manipulatedFlow to complete", e);
                //flowRuntime.stop(true);
            } finally {
                flowRuntime.waitForFlowCompletion();
                flowRuntime.notifyStepsTheFlowIsComplete();
                log.info("Scheduled '{}' on '{}' is finished", deployment.getFlow().toString(),
                        agent.getName());
                executionId = null;
            }
        }
    }

    class AgentRequestHandler implements Runnable {
        @Override
        public void run() {
            synchronized (AgentRuntime.this) {
                configurationService.refresh(agent);
                for (AgentDeployment deployment : new HashSet<AgentDeployment>(agent.getAgentDeployments())) {
                    DeploymentStatus status = deployment.getDeploymentStatus();
                    if (status.equals(DeploymentStatus.REQUEST_DEPLOY)) {
                        deploy(deployment);
                    } else if (status.equals(DeploymentStatus.REQUEST_UNDEPLOY)) {
                        undeploy(deployment);
                    } else if (status.equals(DeploymentStatus.REQUEST_DISABLE)) {
                        stop(deployment);
                        deployment.setStatus(DeploymentStatus.DISABLED.name());
                        configurationService.save(deployment);
                    } else {
                        deployResources(deployment.getFlow());
                    }
                }
                if (agent.getStatus().equals(AgentStatus.REQUEST_REFRESH.name())) {
                    log.info("Agent '" + agent.getName() + "' is refreshing settings");
                    globalSettings = configurationService.findGlobalSettingsAsMap();
                    agent.setStatus(AgentStatus.RUNNING.name());
                    configurationService.save(agent);;
                }
            }
        }
    }

}
