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
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.AgentDeploymentParameter;
import org.jumpmind.metl.core.model.AgentResourceSetting;
import org.jumpmind.metl.core.model.AgentStatus;
import org.jumpmind.metl.core.model.DeploymentStatus;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDependency;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.StartType;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.plugin.XMLResourceDefinition;
import org.jumpmind.metl.core.runtime.component.IComponentDeploymentListener;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.Results;
import org.jumpmind.metl.core.runtime.flow.FlowRuntime;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;
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

    Map<AgentDeployment, ScheduledFuture<?>> scheduledDeployments = Collections.synchronizedMap(new HashMap<>());

    Map<String, IResourceRuntime> deployedResources = Collections.synchronizedMap(new HashMap<>());

    Map<String, String> globalSettings;

    IConfigurationService configurationService;

    IComponentRuntimeFactory componentRuntimeFactory;

    IDefinitionFactory definitionFactory;

    IExecutionService executionService;

    ExecutorService flowStepsExecutionThreads;

    ThreadPoolTaskScheduler flowExecutionScheduler;

    ScheduledFuture<?> agentRequestHandler;

    IHttpRequestMappingRegistry httpRequestMappingRegistry;

    Map<AgentDeployment, List<FlowRuntime>> runningFlows = Collections.synchronizedMap(new HashMap<>());

    public AgentRuntime(Agent agent, IConfigurationService configurationService, IExecutionService executionService,
            IComponentRuntimeFactory componentFactory, IDefinitionFactory definitionFactory,
            IHttpRequestMappingRegistry httpRequestMappingRegistry) {
        this.agent = agent;
        this.definitionFactory = definitionFactory;
        this.executionService = executionService;
        this.configurationService = configurationService;
        this.componentRuntimeFactory = componentFactory;
        this.httpRequestMappingRegistry = httpRequestMappingRegistry;
    }

    public boolean cancel(String executionId) {
        boolean cancelled = false;
        Collection<List<FlowRuntime>> runtimes = runningFlows.values();
        for (List<FlowRuntime> list : runtimes) {
            for (FlowRuntime flowRuntime : list) {
                if (executionId.equals(flowRuntime.getExecutionId())) {
                    if (flowRuntime.isRunning()) {
                        flowRuntime.cancel();
                        cancelled = true;
                        // remove?
                    }
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
                agentName = agentName.substring(0, agentName.length() - 1);
            }
            if (agentName.indexOf(".") > 0) {
                agentName = agentName.substring(0, agentName.indexOf("."));
            }
            final String namePrefix = LogUtils.normalizeName(agentName);

            this.flowStepsExecutionThreads = ThreadUtils.createUnboundedThreadPool(namePrefix);

            this.flowExecutionScheduler = new ThreadPoolTaskScheduler();
            this.flowExecutionScheduler.setDaemon(true);
            this.flowExecutionScheduler.setThreadNamePrefix(namePrefix + "-job-");
            /*
             * Threads are not pre-created. Set this big enough for a typical
             * flow but not too big since every agent gets their own pool.
             * Additional threads can be obtained if the entire pool is used. A
             * common Linux thread limit is 1024 per user.
             */
            this.flowExecutionScheduler.setPoolSize(agent.getExecThreadCount());
            this.flowExecutionScheduler.initialize();

            this.globalSettings = configurationService.findGlobalSettingsAsMap();

            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(agent.getAgentDeployments());
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

            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                stop(deployment, null);
            }

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
            ProjectVersion projectVersion = configurationService.findProjectVersion(flow.getProjectVersionId());
            deployment = new AgentDeployment(flow);
            deployment.setStatus(DeploymentStatus.REQUEST_ENABLE.name());
            deployment.setAgentId(agent.getId());
            deployment.setProjectVersion(projectVersion);
            deployment.setFlow(flow);

            List<FlowParameter> defaultParameters = flow.getFlowParameters();
            for (FlowParameter flowParameter : defaultParameters) {
                String value = flowParameter.getDefaultValue();
                if (parameters != null && parameters.containsKey(flowParameter.getName())) {
                    value = parameters.get(flowParameter.getName());
                }
                deployment.getAgentDeploymentParameters()
                        .add(new AgentDeploymentParameter(flowParameter.getName(), value, deployment.getId(), flowParameter.getId()));

            }

            agent.getAgentDeployments().remove(deployment);
            agent.getAgentDeployments().add(deployment);
            configurationService.save(deployment);

            List<AgentDeployment> deployments = agent.getAgentDeployments();
            deployments.remove(deployment);
            deployments.add(deployment);

            deploy(deployment);
        } else {
            // lets make sure resources are deployed and up to date
            deployResources(deployment.getFlow());
        }
        return deployment;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void deployResources(Flow flow) {
        List<ResourceName> flowResourceNames = new ArrayList<>(configurationService.findResourcesInProject(flow.getProjectVersionId()));
        List<ProjectVersionDependency> dependencies = configurationService.findProjectDependencies(flow.getProjectVersionId());
        for (ProjectVersionDependency projectVersionDependency : dependencies) {
            flowResourceNames.addAll(configurationService.findResourcesInProject(projectVersionDependency.getTargetProjectVersionId()));
        }

        for (ResourceName flowResourceName : flowResourceNames) {
            try {
                Resource flowResource = configurationService.findResource(flowResourceName.getId());
                XMLResourceDefinition definition = definitionFactory.getResourceDefintion(flowResource.getProjectVersionId(),
                        flowResource.getType());

                if (definition != null) {
                    IResourceRuntime alreadyDeployed = deployedResources.get(flowResource.getId());

                    if (alreadyDeployed == null) {
                        Resource previousResource = configurationService.findPreviousVersionResource(flowResource);
                        if (previousResource != null) {
                            log.info("Found a previous version of the '{}' resource.  Using it's agent settings during deployment",
                                    flowResource.getName());
                            TypedProperties previouslyOverriddenSettings = agent.toTypedProperties(definition, previousResource);
                            for (Object key : previouslyOverriddenSettings.keySet()) {
                                AgentResourceSetting setting = new AgentResourceSetting(flowResource.getId(), agent.getId());
                                setting.setName((String) key);
                                setting.setValue((String) previouslyOverriddenSettings.get(key));
                                configurationService.save(setting);
                                agent.getAgentResourceSettings().add(setting);
                            }
                        }
                    }

                    TypedProperties combined = agent.toTypedProperties(definition, flowResource);
                    Set<Entry<Object, Object>> entries = combined.entrySet();
                    for (Entry<Object, Object> entry : entries) {
                        String value = (String) entry.getValue();
                        if (value != null) {
                            value = FormatUtils.replaceTokens(value, (Map) System.getProperties(), true);
                            entry.setValue(value);
                        }
                    }

                    boolean deploy = true;
                    if (alreadyDeployed != null) {
                        deploy = false;
                        TypedProperties alreadyDeployedOverrides = alreadyDeployed.getResourceRuntimeSettings();
                        for (Object key : combined.keySet()) {
                            Object newObj = combined.get(key);
                            Object oldObj = alreadyDeployedOverrides.get(key);
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
                        IResourceRuntime resource = create(definition, flowResource, combined);
                        deployedResources.put(flowResource.getId(), resource);
                    }
                } else {
                    log.error("Could not find a resource definition for deployment for '{}' of type '{}'", flowResourceName.getName(),
                            flowResourceName.getType());
                }
            } catch (Exception e) {
                log.error("Failed to deploy resource '" + flowResourceName.getName() + "' with an id of " + flowResourceName.getId()
                        + " to the '" + agent.getName() + "' agent", e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private IResourceRuntime create(XMLResourceDefinition definition, Resource resource, TypedProperties agentOverrides) {
        try {
            String resourceType = resource.getType();
            Class<? extends IResourceRuntime> clazz = (Class<? extends IResourceRuntime>) definition.getClassLoader()
                    .loadClass(definition.getClassName());
            if (clazz != null) {
                IResourceRuntime runtime = clazz.newInstance();
                runtime.start(resource, agentOverrides);
                return runtime;
            } else {
                throw new IllegalStateException("Could not find a class associated with the resource type of " + resourceType);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void deploy(final AgentDeployment deployment) {
        DeploymentStatus status = deployment.getDeploymentStatus();
        if (!status.equals(DeploymentStatus.DISABLED) && !status.equals(DeploymentStatus.REQUEST_DISABLE)
                && !status.equals(DeploymentStatus.REQUEST_REMOVE)) {
            try {
                log.info("Deploying '{}' to '{}'", deployment.getFlow().toString(), agent.getName());

                deployResources(deployment.getFlow());

                doComponentDeploymentEvent(deployment, (l, f, s, c) -> l.onDeploy(agent, deployment, f, s, c));

                if (deployment.asStartType() == StartType.SCHEDULED_CRON) {
                    String cron = deployment.getStartExpression();
                    log.info("Scheduling '{}' on '{}' with a cron expression of '{}'  The next run time should be at: {}", new Object[] {
                            deployment.getFlow().toString(), agent.getName(), cron, new CronSequenceGenerator(cron).next(new Date()) });

                    ScheduledFuture<?> future = this.flowExecutionScheduler.schedule(new FlowRunner("metl cron", deployment),
                            new CronTrigger(cron));
                    scheduledDeployments.put(deployment, future);
                }

                deployment.setStatus(DeploymentStatus.ENABLED.name());
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

    private void doComponentDeploymentEvent(AgentDeployment deployment, DeployListenerAction method) {
        Flow flow = deployment.getFlow();
        List<FlowStep> steps = flow.getFlowSteps();
        for (FlowStep flowStep : steps) {
            XMLComponentDefinition componentDefintion = definitionFactory.getComponentDefinition(flow.getProjectVersionId(),
                    flowStep.getComponent().getType());
            if (componentDefintion != null && isNotBlank(componentDefintion.getDeploymentListenerClassName())) {
                try {
                    IComponentDeploymentListener listener = (IComponentDeploymentListener) Class
                            .forName(componentDefintion.getDeploymentListenerClassName(), true, componentDefintion.getClassLoader())
                            .newInstance();
                    if (listener instanceof IHttpRequestMappingRegistryAware) {
                        ((IHttpRequestMappingRegistryAware) listener).setHttpRequestMappingRegistry(httpRequestMappingRegistry);
                    }
                    method.run(listener, flow, flowStep, componentDefintion);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public String scheduleNow(String userId, AgentDeployment deployment) {
        return scheduleNow(userId, deployment, null);
    }

    public FlowRuntime createFlowRuntime(String userId, AgentDeployment deployment, Map<String, String> runtimeParameters) throws Exception {
        String executionId = createExecutionId();
        return new FlowRuntime(executionId, userId, deployment, agent, componentRuntimeFactory, definitionFactory, flowStepsExecutionThreads,
                configurationService, executionService, deployedResources, null, globalSettings, runtimeParameters);

    }

    public Results execute(String userId, AgentDeployment deployment, Map<String, String> runtimeParameters) throws Exception {
        log.info("Executing '{}' on '{}' for now", new Object[] { deployment.getName(), agent.getName() });
        return createFlowRuntime(userId, deployment, runtimeParameters).execute();
    }

    public String scheduleNow(String userId, AgentDeployment deployment, Map<String, String> runtimeParameters) {
        log.info("Scheduling '{}' on '{}' for now", new Object[] { deployment.getName(), agent.getName() });
        String executionId = createExecutionId();
        this.flowExecutionScheduler.schedule(new FlowRunner(userId, deployment, runtimeParameters, executionId), new Date());
        return executionId;
    }

    protected void stop(AgentDeployment deployment, DeploymentStatus nextStatus) {
        ScheduledFuture<?> future = scheduledDeployments.get(deployment);
        if (future != null) {
            future.cancel(true);
            scheduledDeployments.remove(future);
        }

        List<FlowRuntime> flowRuntimes = runningFlows.get(deployment);
        if (flowRuntimes != null) {
            for (FlowRuntime flowRuntime : flowRuntimes) {
                if (flowRuntime != null) {
                    try {
                        flowRuntime.cancel();
                        log.info("Flow '{}' has been undeployed", deployment.getFlow().getName());
                    } catch (Exception e) {
                        log.warn("Failed to stop '{}'", deployment.getFlow().getName(), e);
                    }
                }
            }
        }

        if (nextStatus != null) {
            deployment.setStatus(nextStatus.name());
            configurationService.save(deployment);
        }

    }

    public Collection<IResourceRuntime> getDeployedResources() {
        return new HashSet<IResourceRuntime>(deployedResources.values());
    }

    public synchronized void undeploy(AgentDeployment deployment) {
        doComponentDeploymentEvent(deployment, (l, f, s, c) -> l.onUndeploy(agent, deployment, f, s, c));
        stop(deployment, null);
        configurationService.delete(deployment);
        agent.getAgentDeployments().remove(deployment);
    }

    private void removeFromRunning(AgentDeployment deployment, FlowRuntime flowRuntime) {
        List<FlowRuntime> flows = runningFlows.get(deployment);
        if (flows != null) {
            flows.remove(flowRuntime);
        }
    }

    private void addToRunning(AgentDeployment deployment, FlowRuntime flowRuntime) {
        List<FlowRuntime> flows = runningFlows.get(deployment);
        if (flows == null) {
            synchronized (runningFlows) {
                flows = new ArrayList<>();
                runningFlows.put(deployment, flows);
            }
        }
        flows.add(flowRuntime);
    }

    private final String createExecutionId() {
        return UUID.randomUUID().toString();
    }

    class FlowRunner implements Runnable {

        String executionId;

        Map<String, String> runtimeParameters;

        AgentDeployment deployment;

        String userId;

        public FlowRunner(String userId, AgentDeployment deployment) {
            this(userId, deployment, null, null);
        }

        public FlowRunner(String userId, AgentDeployment deployment, Map<String, String> runtimeParameters) {
            this(userId, deployment, runtimeParameters, null);
        }

        public FlowRunner(String userId, AgentDeployment deployment, Map<String, String> runtimeParameters, String executionId) {
            this.userId = userId;
            this.executionId = executionId;
            this.runtimeParameters = runtimeParameters;
            this.deployment = deployment;
        }

        @Override
        public void run() {
            if (isBlank(executionId)) {
                executionId = createExecutionId();
            }
            FlowRuntime flowRuntime = null;
            try {
                log.info("Deployment '{}' is running on the '{}' agent", deployment.getName(), agent.getName());
                List<Notification> notifications = configurationService.findNotificationsForDeployment(deployment);
                flowRuntime = new FlowRuntime(executionId, userId, deployment, agent, componentRuntimeFactory, definitionFactory,
                        flowStepsExecutionThreads, configurationService, executionService, deployedResources, notifications, globalSettings,
                        runtimeParameters);
                addToRunning(deployment, flowRuntime);
                flowRuntime.execute();
            } catch (Exception e) {
                log.error("Error while waiting for the flow to complete", e);
            } finally {
                removeFromRunning(deployment, flowRuntime);
                log.info("Scheduled '{}' on '{}' is finished", deployment.getFlow().toString(), agent.getName());
                executionId = null;
            }
        }
    }

    class AgentRequestHandler implements Runnable {
        @Override
        public void run() {
            synchronized (AgentRuntime.this) {
                agent = configurationService.findAgent(agent.getId(), true);
                for (AgentDeployment deployment : new HashSet<AgentDeployment>(agent.getAgentDeployments())) {
                    DeploymentStatus status = deployment.getDeploymentStatus();
                    if (status.equals(DeploymentStatus.REQUEST_ENABLE)) {
                        deploy(deployment);
                    } else if (status.equals(DeploymentStatus.REQUEST_REMOVE)) {
                        undeploy(deployment);
                    } else if (status.equals(DeploymentStatus.REQUEST_DISABLE)) {
                        stop(deployment, DeploymentStatus.DISABLED);
                    } else if (status.equals(DeploymentStatus.REQUEST_REENABLE)) {
                        stop(deployment, DeploymentStatus.REQUEST_ENABLE);
                    } else {
                        deployResources(deployment.getFlow());
                    }
                }
                if (agent.getStatus().equals(AgentStatus.REQUEST_REFRESH.name())) {
                    log.info("Agent '" + agent.getName() + "' is refreshing settings");
                    globalSettings = configurationService.findGlobalSettingsAsMap();
                    agent.setStatus(AgentStatus.RUNNING.name());
                    configurationService.save(agent);
                    ;
                }
            }
        }
    }

    interface DeployListenerAction {
        public void run(IComponentDeploymentListener listener, Flow flow, FlowStep step, XMLComponentDefinition componentDefintion)
                throws Exception;
    }

}
