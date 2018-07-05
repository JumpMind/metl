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
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.AgentFlowDeployParm;
import org.jumpmind.metl.core.model.AgentProjectVersionFlowDeployment;
import org.jumpmind.metl.core.model.AgentStatus;
import org.jumpmind.metl.core.model.DeploymentStatus;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.StartType;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.plugin.XMLResourceDefinition;
import org.jumpmind.metl.core.runtime.component.IComponentDeploymentListener;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.Results;
import org.jumpmind.metl.core.runtime.flow.FlowRuntime;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.subscribe.ISubscribeManager;
import org.jumpmind.metl.core.runtime.subscribe.ISubscribeManagerAware;
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

    Map<AgentDeploy, ScheduledFuture<?>> scheduledDeployments = Collections.synchronizedMap(new HashMap<>());

    Map<String, IResourceRuntime> deployedResources = Collections.synchronizedMap(new HashMap<>());

    Set<AgentProjectVersionFlowDeployment> deployed = Collections.synchronizedSet(new HashSet<>());

    Map<String, String> globalSettings;

    IConfigurationService configurationService;

    IComponentRuntimeFactory componentRuntimeFactory;

    IDefinitionFactory definitionFactory;

    IExecutionService executionService;

    ExecutorService flowStepsExecutionThreads;

    ThreadPoolTaskScheduler flowExecutionScheduler;

    ScheduledFuture<?> agentRequestHandler;

    IHttpRequestMappingRegistry httpRequestMappingRegistry;

    IOperationsService operationsService;
    
    ISubscribeManager subscribeManager;

    Map<AgentDeploy, List<FlowRuntime>> runningFlows = Collections.synchronizedMap(new HashMap<>());

    public AgentRuntime(Agent agent, IOperationsService operationsService, IConfigurationService configurationService,
            IExecutionService executionService, IComponentRuntimeFactory componentFactory, IDefinitionFactory definitionFactory,
            IHttpRequestMappingRegistry httpRequestMappingRegistry, ISubscribeManager subscribeManager) {
        this.operationsService = operationsService;
        this.agent = agent;
        this.definitionFactory = definitionFactory;
        this.executionService = executionService;
        this.configurationService = configurationService;
        this.componentRuntimeFactory = componentFactory;
        this.httpRequestMappingRegistry = httpRequestMappingRegistry;
        this.subscribeManager = subscribeManager;
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

            this.globalSettings = operationsService.findGlobalSettingsAsMap();

            List<AgentDeploy> deployments = new ArrayList<AgentDeploy>(agent.getAgentDeployments());
            for (AgentDeploy deployment : deployments) {
                deploy(deployment);
            }

            agentRequestHandler = this.flowExecutionScheduler.scheduleWithFixedDelay(new AgentRequestHandler(), 10000);

            agent.setAgentStatus(AgentStatus.RUNNING);
            operationsService.save(agent);

            started = true;
            starting = false;
            log.info("Agent '{}' has been started", agent);
            
            // loop through the deployed agents and start those that are set to run on startup
            for (AgentDeploy deployment : deployments) {
                Flow flow = configurationService.findFlow(deployment.getFlowId());
                ProjectVersion projectVersion = configurationService.findProjectVersion(flow.getProjectVersionId());
                if (!projectVersion.isDeleted()) {
                	DeploymentStatus status = deployment.getDeploymentStatus();
                    if (!status.equals(DeploymentStatus.DISABLED) && !status.equals(DeploymentStatus.REQUEST_DISABLE)
                            && !status.equals(DeploymentStatus.REQUEST_REMOVE)) {
                        try {
            	            if (deployment.asStartType() == StartType.ON_STARTUP) {
            	            	scheduleNow("system startup", deployment);
            	            }
                        } catch (Exception e) {
                            log.warn("Failed to start '{}'", deployment.getName(), e);
                            deployment.setStatus(DeploymentStatus.ERROR.name());
                            deployment.setMessage(ExceptionUtils.getRootCauseMessage(e));
                        }
                    }
                } 
            }
        }
    }

    protected AgentProjectVersionFlowDeployment createAgentProjectVersionFlowDeployment(AgentDeploy deployment) {
        Flow flow = configurationService.findFlow(deployment.getFlowId());
        ProjectVersion projectVersion = configurationService.findProjectVersion(flow.getProjectVersionId());
        return new AgentProjectVersionFlowDeployment(deployment, flow, projectVersion);
    }

    public synchronized void stop() {
        if (started && !stopping) {
            stopping = true;

            agentRequestHandler.cancel(true);

            List<AgentDeploy> deployments = new ArrayList<AgentDeploy>(agent.getAgentDeployments());
            for (AgentDeploy deployment : deployments) {
                stop(deployment, null);
            }

            agent.setAgentStatus(AgentStatus.STOPPED);
            operationsService.save(agent);

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
            
            deployedResources.clear();

            log.info("Agent '{}' has been stopped", agent);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public synchronized AgentDeploy deploy(Flow flow, Map<String, String> parameters) {
        AgentDeploy deployment = agent.getAgentDeploymentFor(flow);
        if (deployment == null) {
            ProjectVersion projectVersion = configurationService.findProjectVersion(flow.getProjectVersionId());
            deployment = new AgentDeploy();
            deployment.setFlowId(flow.getId());
            deployment.setName(flow.getName());
            deployment.setStatus(DeploymentStatus.REQUEST_ENABLE.name());
            deployment.setAgentId(agent.getId());

            List<FlowParameter> defaultParameters = flow.getFlowParameters();
            for (FlowParameter flowParameter : defaultParameters) {
                String value = flowParameter.getDefaultValue();
                if (parameters != null && parameters.containsKey(flowParameter.getName())) {
                    value = parameters.get(flowParameter.getName());
                }
                deployment.getAgentDeploymentParms()
                        .add(new AgentFlowDeployParm(flowParameter.getName(), value, deployment.getId(), flowParameter.getId()));

            }

            agent.getAgentDeployments().remove(deployment);
            agent.getAgentDeployments().add(deployment);
            operationsService.save(deployment);

            List<AgentDeploy> deployments = agent.getAgentDeployments();
            deployments.remove(deployment);
            deployments.add(deployment);

            deploy(deployment, flow, projectVersion);
        } else {
            // lets make sure resources are deployed and up to date
            deployResources(flow);
        }
        return deployment;
    }

    protected void deploy(AgentDeploy deployment) {
        Flow flow = configurationService.findFlow(deployment.getFlowId());
        ProjectVersion projectVersion = configurationService.findProjectVersion(flow.getProjectVersionId());
        if (!projectVersion.isDeleted()) {
            deploy(deployment, flow, projectVersion);
        } else {
            log.warn("Failed to deploy the flow '{}' from the project version '{}:{}' because the project or version had been deleted",
                    flow.getName(), projectVersion.getProject().getName(), projectVersion.getVersionLabel());
        }
    }

    private void deploy(AgentDeploy deployment, Flow flow, ProjectVersion projectVersion) {
        DeploymentStatus status = deployment.getDeploymentStatus();
        if (!status.equals(DeploymentStatus.DISABLED) && !status.equals(DeploymentStatus.REQUEST_DISABLE)
                && !status.equals(DeploymentStatus.REQUEST_REMOVE)) {
            try {
                log.info("Deploying '{}' to '{}'", deployment.getName(), agent.getName());

                deployResources(flow);

                AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment = new AgentProjectVersionFlowDeployment(deployment, flow,
                        projectVersion);

                doComponentDeploymentEvent(agentProjectVersionFlowDeployment,
                        (l, f, s, c) -> l.onDeploy(agent, agentProjectVersionFlowDeployment, s, c));

                if (deployment.asStartType() == StartType.SCHEDULED_CRON) {
                    String cron = deployment.getStartExpression();
                    log.info("Scheduling '{}' on '{}' with a cron expression of '{}'  The next run time should be at: {}",
                            new Object[] { flow.getName(), agent.getName(), cron, new CronSequenceGenerator(cron).next(new Date()) });

                    ScheduledFuture<?> future = this.flowExecutionScheduler
                            .schedule(new FlowRunner("metl cron", agentProjectVersionFlowDeployment), new CronTrigger(cron));
                    scheduledDeployments.put(deployment, future);
                }

                deployment.setStatus(DeploymentStatus.ENABLED.name());
                deployment.setMessage("");
                deployed.add(agentProjectVersionFlowDeployment);
                log.info("Flow '{}' has been deployed", deployment.getName());
            } catch (Exception e) {
                log.warn("Failed to start '{}'", deployment.getName(), e);
                deployment.setStatus(DeploymentStatus.ERROR.name());
                deployment.setMessage(ExceptionUtils.getRootCauseMessage(e));
            }
            operationsService.save(deployment);
        }
    }

    protected void deployResources(AgentDeploy deployment) {
        Flow flow = configurationService.findFlow(deployment.getFlowId());
        deployResources(flow);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void deployResources(Flow flow) {
        List<ResourceName> flowResourceNames = new ArrayList<>(configurationService.findResourcesInProject(flow.getProjectVersionId()));
        List<ProjectVersionDepends> dependencies = configurationService.findProjectDependencies(flow.getProjectVersionId());
        for (ProjectVersionDepends projectVersionDependency : dependencies) {
            flowResourceNames.addAll(configurationService.findResourcesInProject(projectVersionDependency.getTargetProjectVersionId()));
        }

        for (ResourceName flowResourceName : flowResourceNames) {
            try {
                Resource flowResource = configurationService.findResource(flowResourceName.getId());
                XMLResourceDefinition definition = definitionFactory.getResourceDefintion(flowResource.getProjectVersionId(),
                        flowResource.getType());

                if (definition != null) {
                    IResourceRuntime alreadyDeployed = deployedResources.get(flowResource.getId());

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
    public static IResourceRuntime create(XMLResourceDefinition definition, Resource resource, TypedProperties agentOverrides) { // ADB
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

    private void doComponentDeploymentEvent(AgentProjectVersionFlowDeployment deployment, DeployListenerAction method) {
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
                    if (listener instanceof ISubscribeManagerAware) {
                        ((ISubscribeManagerAware) listener).setSubscribeManager(subscribeManager);
                    }
                    method.run(listener, flow, flowStep, componentDefintion);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public String scheduleNow(String userId, AgentDeploy deployment) {
        return scheduleNow(userId, deployment, null);
    }

    public FlowRuntime createFlowRuntime(String userId, AgentDeploy deployment, Map<String, String> runtimeParameters) throws Exception {
        String executionId = createExecutionId();
        return new FlowRuntime(executionId, userId, findDeployed(deployment), agent, componentRuntimeFactory, definitionFactory,
                flowStepsExecutionThreads, operationsService, configurationService, executionService, deployedResources, null, globalSettings,
                runtimeParameters);
    }

    public Results execute(String userId, AgentDeploy deployment, Map<String, String> runtimeParameters) throws Exception {
        log.info("Executing '{}' on '{}' for now", new Object[] { deployment.getName(), agent.getName() });
        return createFlowRuntime(userId, deployment, runtimeParameters).execute();
    }

    public String scheduleNow(String userId, AgentDeploy deployment, Map<String, String> runtimeParameters) {
        log.info("Scheduling '{}' on '{}' for now", new Object[] { deployment.getName(), agent.getName() });
        if (agent.isAutoRefresh()) {
            deployResources(deployment);
        }
        String executionId = createExecutionId();
        this.flowExecutionScheduler.schedule(new FlowRunner(userId, findDeployed(deployment), runtimeParameters, executionId), new Date());
        return executionId;
    }

    protected void stop(AgentDeploy deployment, DeploymentStatus nextStatus) {
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
                        logWithFlowName(LogLevel.INFO, "Flow '{}' has been cancelled", deployment, null);
                    } catch (Exception e) {
                        logWithFlowName(LogLevel.WARN, "Failed to stop '{}'", deployment, e);
                    }
                }
            }
        }
        
        AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment = findDeployed(deployment);
        if (agentProjectVersionFlowDeployment != null) {
            doComponentDeploymentEvent(agentProjectVersionFlowDeployment,
                    (l, f, s, c) -> l.onUndeploy(agent, agentProjectVersionFlowDeployment, s, c));
        }


        if (nextStatus != null) {
            deployment.setStatus(nextStatus.name());
            operationsService.save(deployment);
        }

    }

    private final void logWithFlowName(LogLevel level, String msg, AgentDeploy deployment, Exception e) {
        AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment = findDeployed(deployment);
        if (agentProjectVersionFlowDeployment != null) {
            if (LogLevel.INFO == level) {
                log.info(msg, agentProjectVersionFlowDeployment.getFlow().getName());
            } else if (LogLevel.WARN == level) {
                log.warn(msg, agentProjectVersionFlowDeployment.getFlow().getName(), e);
            }
        }
    }
    
    public IResourceRuntime getDeployedResource(String id) {
        return deployedResources.get(id);
    }

    public Collection<IResourceRuntime> getDeployedResources() {
        return new HashSet<IResourceRuntime>(deployedResources.values());
    }

    protected AgentProjectVersionFlowDeployment findDeployed(AgentDeploy deployment) {
        for (AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment : deployed) {
            if (agentProjectVersionFlowDeployment.getAgentDeployment().equals(deployment)) {
                return agentProjectVersionFlowDeployment;
            }
        }
        return null;
    }

    public synchronized void undeploy(AgentDeploy deployment) {
        AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment = findDeployed(deployment);
        if (agentProjectVersionFlowDeployment != null) {
            stop(deployment, null);
        }
        operationsService.delete(deployment);
        agent.getAgentDeployments().remove(deployment);
    }

    private void removeFromRunning(AgentDeploy deployment, FlowRuntime flowRuntime) {
        List<FlowRuntime> flows = runningFlows.get(deployment);
        if (flows != null) {
            flows.remove(flowRuntime);
        }
    }

    private void addToRunning(AgentDeploy deployment, FlowRuntime flowRuntime) {
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

        AgentProjectVersionFlowDeployment deployment;

        String userId;

        public FlowRunner(String userId, AgentProjectVersionFlowDeployment deployment) {
            this(userId, deployment, null, null);
        }

        public FlowRunner(String userId, AgentProjectVersionFlowDeployment deployment, Map<String, String> runtimeParameters) {
            this(userId, deployment, runtimeParameters, null);
        }

        public FlowRunner(String userId, AgentProjectVersionFlowDeployment deployment, Map<String, String> runtimeParameters,
                String executionId) {
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
                List<Notification> notifications = operationsService.findNotificationsForDeployment(deployment.getAgentDeployment());
                flowRuntime = new FlowRuntime(executionId, userId, deployment, agent, componentRuntimeFactory, definitionFactory,
                        flowStepsExecutionThreads, operationsService, configurationService, executionService, deployedResources,
                        notifications, globalSettings, runtimeParameters);
                addToRunning(deployment.getAgentDeployment(), flowRuntime);
                flowRuntime.execute();
            } catch (Exception e) {
                log.error("Error while waiting for the flow to complete", e);
            } finally {
                if (deployment != null) {
                    removeFromRunning(deployment.getAgentDeployment(), flowRuntime);
                    AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment = findDeployed(deployment.getAgentDeployment());
                    if (agentProjectVersionFlowDeployment != null) {
                        log.info("Scheduled '{}' on '{}' is finished", agentProjectVersionFlowDeployment.getFlow().getName(),
                                agent.getName());
                    }
                }
                executionId = null;                
            }
        }
    }

    class AgentRequestHandler implements Runnable {
        @Override
        public void run() {
            synchronized (AgentRuntime.this) {
                agent = operationsService.findAgent(agent.getId(), true);
                for (AgentDeploy deployment : new HashSet<AgentDeploy>(agent.getAgentDeployments())) {
                    FlowName flow = configurationService.findFlowName(deployment.getFlowId());
                    ProjectVersion projectVersion = configurationService.findProjectVersion(flow.getProjectVersionId());
                    if (!projectVersion.isDeleted()) {
                        DeploymentStatus status = deployment.getDeploymentStatus();
                        if (status.equals(DeploymentStatus.REQUEST_ENABLE)) {
                            deploy(deployment);
                        } else if (status.equals(DeploymentStatus.REQUEST_REMOVE)) {
                            undeploy(deployment);
                        } else if (status.equals(DeploymentStatus.REQUEST_DISABLE)) {
                            stop(deployment, DeploymentStatus.DISABLED);
                        } else if (status.equals(DeploymentStatus.REQUEST_REENABLE)) {
                            stop(deployment, DeploymentStatus.REQUEST_ENABLE);
                        }
                    }
                }
                if (agent.getStatus().equals(AgentStatus.REQUEST_REFRESH.name())) {
                    log.info("Agent '" + agent.getName() + "' is refreshing settings");
                    globalSettings = operationsService.findGlobalSettingsAsMap();
                    agent.setStatus(AgentStatus.RUNNING.name());
                    operationsService.save(agent);
                }
            }
        }
    }

    interface DeployListenerAction {
        public void run(IComponentDeploymentListener listener, Flow flow, FlowStep step, XMLComponentDefinition componentDefintion)
                throws Exception;
    }

}
