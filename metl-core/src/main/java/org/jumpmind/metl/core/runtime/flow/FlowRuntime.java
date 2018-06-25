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
package org.jumpmind.metl.core.runtime.flow;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentFlowDeployParm;
import org.jumpmind.metl.core.model.AgentParameter;
import org.jumpmind.metl.core.model.AgentProjectVersionFlowDeployment;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.ExecutionTrackerLogger;
import org.jumpmind.metl.core.runtime.ExecutionTrackerRecorder;
import org.jumpmind.metl.core.runtime.IExecutionTracker;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.core.runtime.component.ComponentContext;
import org.jumpmind.metl.core.runtime.component.ComponentStatistics;
import org.jumpmind.metl.core.runtime.component.IComponentRuntime;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.IHasResults;
import org.jumpmind.metl.core.runtime.component.IHasSecurity;
import org.jumpmind.metl.core.runtime.component.Results;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.MailSession;
import org.jumpmind.util.AppUtils;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowRuntime {

    final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DATE_FORMAT = "yyyy-MM-dd";

    private static final String TIME_FORMAT = "HH:mm:ss";

    AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment;

    Map<FlowStep, IComponentRuntime> endpointRuntimes = new HashMap<FlowStep, IComponentRuntime>();

    Map<String, IResourceRuntime> resourceRuntimes = new HashMap<String, IResourceRuntime>();

    IComponentRuntimeFactory componentRuntimeFactory;

    IDefinitionFactory definitionFactory;

    IExecutionTracker executionTracker;

    ExecutorService threadService;

    Map<String, StepRuntime> stepRuntimes;

    Agent agent;

    Map<String, String> flowParameters;
    
    Map<String, String> flowVariables;

    List<Notification> notifications;

    IConfigurationService configurationService;

    IExecutionService executionService;

    String executionId;

    Map<String, String> globalSettings;
    
    Map<String, IResourceRuntime> deployedResources;
    
    Flow manipulatedFlow;

    public FlowRuntime(String executionId, String userId, AgentProjectVersionFlowDeployment deployment, Agent agent,
            IComponentRuntimeFactory componentRuntimeFactory,
            IDefinitionFactory componentDefinitionFactory,            
            ExecutorService threadService,
            IOperationsService operationsService,
            IConfigurationService configurationService, IExecutionService executionService,
            Map<String, IResourceRuntime> deployedResources, List<Notification> notifications,
            Map<String, String> globalSettings) {
        this(executionId, userId, deployment, agent, componentRuntimeFactory, componentDefinitionFactory,
                threadService, operationsService, configurationService, executionService,
                deployedResources, notifications, globalSettings, null);
    }

    public FlowRuntime(String executionId, String userId, AgentProjectVersionFlowDeployment deployment, Agent agent,
            IComponentRuntimeFactory componentRuntimeFactory,
            IDefinitionFactory definitionFactory,
            ExecutorService threadService, IOperationsService operationsService,
            IConfigurationService configurationService, IExecutionService executionService,
            Map<String, IResourceRuntime> deployedResources, List<Notification> notifications,
            Map<String, String> globalSettings, Map<String, String> runtimeParameters) {
        
        if (agent.isAutoRefresh() && configurationService != null && operationsService != null) {
            deployment.setFlow(configurationService.findFlow(deployment.getFlow().getId()));
            deployment.setAgentDeployment(operationsService.findAgentDeployment(deployment.getAgentDeployment().getId()));
            operationsService.refreshAgentParameters(agent);
        }
        this.executionId = executionId;
        this.agentProjectVersionFlowDeployment = deployment;
        this.agent = agent;
        this.notifications = notifications;
        this.componentRuntimeFactory = componentRuntimeFactory;
        this.definitionFactory = definitionFactory;
        this.threadService = threadService;
        this.configurationService = configurationService;
        this.executionService = executionService;
        this.deployedResources = deployedResources;
        this.globalSettings = globalSettings;
        
        this.flowParameters = getFlowParameters(agent, deployment);
        if (runtimeParameters != null) {
            this.flowParameters.putAll(runtimeParameters);
        }
        this.flowVariables = Collections.synchronizedMap(new HashMap<>());
        
        if (threadService != null && executionService != null) {
            this.executionTracker = new ExecutionTrackerRecorder(agent, deployment, threadService,
                    executionService, userId, flowParameters.toString());
        } else {
            this.executionTracker = new ExecutionTrackerLogger(deployment);
        }
        this.stepRuntimes = new HashMap<String, StepRuntime>();

        manipulatedFlow = manipulateFlow(deployment.getFlow());
        
        /* create a step runtime for every component in the flow */
        for (FlowStep flowStep : manipulatedFlow.getFlowSteps()) {
            boolean enabled = flowStep.getComponent().getBoolean(AbstractComponentRuntime.ENABLED,
                    true);
            if (enabled) {
                ComponentContext context = new ComponentContext(deployment.getAgentDeployment(), flowStep,
                        manipulatedFlow, executionTracker, deployedResources, flowParameters,
                        globalSettings, flowVariables);
                StepRuntime stepRuntime = new StepRuntime(componentRuntimeFactory,
                        definitionFactory, context, this);
                stepRuntimes.put(flowStep.getId(), stepRuntime);
            }
        }

        List<FlowStepLink> links = manipulatedFlow.getFlowStepLinks();

        /* for each step runtime, set their list of msgTarget step runtimes */
        for (String stepId : stepRuntimes.keySet()) {
            List<StepRuntime> targetStepRuntimes = new ArrayList<StepRuntime>();
            List<StepRuntime> sourceStepRuntimes = new ArrayList<StepRuntime>();
            for (FlowStepLink flowStepLink : links) {
                if (stepId.equals(flowStepLink.getSourceStepId())) {
                    StepRuntime runtime = stepRuntimes.get(flowStepLink.getTargetStepId());
                    if (runtime != null) {
                        targetStepRuntimes.add(runtime);
                    }
                }
                if (stepId.equals(flowStepLink.getTargetStepId())) {
                    StepRuntime runtime = stepRuntimes.get(flowStepLink.getSourceStepId());
                    if (runtime != null) {
                        sourceStepRuntimes.add(runtime);
                    }
                }
            }
            StepRuntime runtime = stepRuntimes.get(stepId);
            if (runtime != null) {
                runtime.setTargetStepRuntimes(targetStepRuntimes);
                runtime.setSourceStepRuntimes(sourceStepRuntimes);
            }
        }

        /* start up each step runtime */
        manipulatedFlow.calculateApproximateOrder();        
    }

    public AgentProjectVersionFlowDeployment getAgentProjectVersionFlowDeployment() {
        return agentProjectVersionFlowDeployment;
    }
    
    public IHasSecurity getHasSecurity() {
        IHasSecurity security = null;
        Collection<StepRuntime> steps = stepRuntimes.values();
        for (StepRuntime stepRuntime : steps) {
            List<IComponentRuntime> runtimes = stepRuntime.getComponentRuntimes();
            for (IComponentRuntime runtime : runtimes) {
                if (runtime instanceof IHasSecurity) {
                    security = ((IHasSecurity) runtime);
                }
            }            
        }
        return security;
    }

    public Results getResult() {
        Results response = null;
        Collection<StepRuntime> steps = stepRuntimes.values();
        for (StepRuntime stepRuntime : steps) {
            List<IComponentRuntime> runtimes = stepRuntime.getComponentRuntimes();
            for (IComponentRuntime runtime : runtimes) {
                if (runtime instanceof IHasResults) {
                    response = ((IHasResults) runtime).getResults();
                }
            }
        }
        return response;
    }

    public Results execute() throws Exception {
        try {
            start();
        } catch (Exception ex) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw new RuntimeException(ex);
            }
        } finally {
            waitForFlowCompletion();
            notifyStepsTheFlowIsComplete();
        }

        List<Throwable> errors = getAllErrors();
        if (errors.size() == 0) {
            return getResult();
        } else {
            if (errors.size() == 1) {
                Throwable throwable = errors.get(0);
                if (throwable instanceof Exception) {
                    throw (Exception) throwable;
                } else if (throwable instanceof Error) {
                    throw (Error) throwable;
                } else {
                    throw new RuntimeException(throwable);
                }
            }
            throw new Exception(getErrorText(errors));
        }
    }

    public void start() throws InterruptedException {

        executionTracker.beforeFlow(executionId, flowParameters);

        sendNotifications(Notification.EventType.FLOW_START);

        List<StepRuntime> startSteps = findStartSteps();

        List<FlowStep> flowSteps = manipulatedFlow.getFlowSteps();
        for (FlowStep flowStep : flowSteps) {
            StepRuntime stepRuntime = stepRuntimes.get(flowStep.getId());
            if (stepRuntime != null) {
                try {
                    stepRuntime.start();
                } catch (RuntimeException ex) {
                    stepRuntime.error = ex;
                    throw ex;
                }
            }
        }

        /* each step is started as a thread */
        for (StepRuntime stepRuntime : stepRuntimes.values()) {
            stepRuntime.startRunning();
            threadService.execute(stepRuntime);
        }

        ControlMessage startMessage = new ControlMessage();
        /*
         * for each start step (step that has no input msgs), send a start
         * message to that step
         */
        for (StepRuntime stepRuntime : startSteps) {
            stepRuntime.queue(startMessage);
        }
    }

    protected Flow manipulateFlow(Flow flow) {        
        Flow clone = (Flow)flow.clone();
        clone.setFlowParameters(new ArrayList<>());
        clone.getFlowParameters().addAll(flow.getFlowParameters());
        clone.setFlowSteps(new ArrayList<>());
        clone.getFlowSteps().addAll(flow.getFlowSteps());
        clone.setFlowStepLinks(new ArrayList<>());
        clone.getFlowStepLinks().addAll(flow.getFlowStepLinks());

        for (FlowStep flowStep : new ArrayList<>(clone.getFlowSteps())) {
            XMLComponentDefinition componentDefintion = definitionFactory.getComponentDefinition(flow.getProjectVersionId(), flowStep.getComponent().getType());
            if (isNotBlank(componentDefintion.getFlowManipulatorClassName())) {
                try {
                    IFlowManipulator flowManipulator = (IFlowManipulator) Class
                            .forName(componentDefintion.getFlowManipulatorClassName())
                            .newInstance();
                    clone = flowManipulator.manipulate(clone, flowStep, configurationService);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return clone;
    }

    public static Map<String, String> getFlowParameters(Agent agent,
            AgentProjectVersionFlowDeployment agentDeployment) {
        Map<String, String> params = new HashMap<String, String>();
        List<FlowParameter> flowParameters = agentDeployment.getFlow().getFlowParameters();
        for (FlowParameter flowParameter : flowParameters) {
            params.put(flowParameter.getName(), flowParameter.getDefaultValue());
        }
        
        List<AgentFlowDeployParm> deployParameters = agentDeployment.getAgentDeployment()
                .getAgentDeploymentParms();
        List<AgentParameter> agentParameters = agent.getAgentParameters();
        if (agentParameters != null) {
            for (AgentParameter agentParameter : agentParameters) {
                if (isNotBlank(agentParameter.getValue())) {
                    params.put(agentParameter.getName(), agentParameter.getValue());
                }
            }
        }
        if (deployParameters != null) {
            for (AgentFlowDeployParm deployParameter : deployParameters) {
                if (isNotBlank(deployParameter.getValue())) {
                    params.put(deployParameter.getName(), deployParameter.getValue());
                }
            }
        }
        Date date = new Date();
        params.put("_agentName", agent.getName());
        params.put("_deploymentName", agentDeployment.getName());        
        try {
            params.put("_agentNameUrlEncoded", URLEncoder.encode(agent.getName(), "utf-8"));
            params.put("_deploymentNameUrlEncoded", URLEncoder.encode(agentDeployment.getName(), "utf-8"));
        } catch (UnsupportedEncodingException e) {
        }
        params.put("_versionName", agentDeployment.getProjectVersion().getVersionLabel());
        params.put("_flowName", agentDeployment.getFlow().getName());
        params.put("_host", AppUtils.getHostName());
        params.put("_date", DateFormatUtils.format(date, DATE_FORMAT));
        params.put("_time", DateFormatUtils.format(date, TIME_FORMAT));
        params.put("_startDate", DateFormatUtils.format(date, DATE_FORMAT));
        params.put("_startTime", DateFormatUtils.format(date, TIME_FORMAT));
        return params;
    }

    /*
     * Waiting until all steps have exited
     */
    public void waitForFlowCompletion() {
        while (isRunning()) {
            AppUtils.sleep(5);
        }
    }

    public void notifyStepsTheFlowIsComplete() {
        List<Throwable> allErrors = getAllErrors(); 

        Collection<StepRuntime> allSteps = stepRuntimes.values();
        for (StepRuntime stepRuntime : allSteps) {
            if (allErrors.size() > 0) {
                stepRuntime.flowCompletedWithErrors(stepRuntime.getError(), allErrors);
            } else {
                stepRuntime.flowCompletedWithoutError();
            }
        }

        executionTracker.afterFlow();

        allErrors = getAllErrors();
        // Check getAllErrors here to make sure any new errors are trapped from
        // the flowCompleted methods
        if (allErrors.size() > 0) {
            flowParameters.put("_errorText", getErrorText(allErrors));
            sendNotifications(Notification.EventType.FLOW_ERROR);
        }
        sendNotifications(Notification.EventType.FLOW_END);
    }

    public List<Throwable> getAllErrors() {
        Collection<StepRuntime> allSteps = stepRuntimes.values();
        List<Throwable> allErrors = new ArrayList<Throwable>();
        for (StepRuntime stepRuntime : allSteps) {
            if (stepRuntime.getError() != null) {
                allErrors.add(stepRuntime.getError());
            }
        }
        return allErrors;
    }

    public String getErrorText(List<Throwable> allErrors) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (Throwable error : allErrors) {
            if (++count < 10) {
                sb.append(ExceptionUtils.getStackTrace(error)).append("\n");
            } else {
                sb.append("\n...and ").append(allErrors.size() - 10).append(" more errors...");
                break;
            }
        }
        return sb.toString();
    }

    public boolean isRunning() {
        boolean running = false;
        if (stepRuntimes != null) {
            Collection<StepRuntime> allSteps = stepRuntimes.values();
            for (StepRuntime stepRuntime : allSteps) {
                if (stepRuntime.isRunning()) {
                    running = true;
                }
            }
        }
        return running;
    }

    protected List<StepRuntime> findStartSteps() {
        List<StepRuntime> starterSteps = new ArrayList<StepRuntime>();
        for (String stepId : stepRuntimes.keySet()) {
            List<FlowStepLink> links = manipulatedFlow.getFlowStepLinks();
            boolean isTargetStep = false;
            for (FlowStepLink flowStepLink : links) {
                if (stepRuntimes.get(flowStepLink.getSourceStepId()) != null
                        && stepId.equals(flowStepLink.getTargetStepId())) {
                    isTargetStep = true;
                }
            }

            if (!isTargetStep) {
                stepRuntimes.get(stepId).getComponentContext().setStartStep(true);
                starterSteps.add(stepRuntimes.get(stepId));
            }
        }
        return starterSteps;
    }

    public void cancel() {
        if (stepRuntimes != null) {
            for (StepRuntime stepRuntime : stepRuntimes.values()) {
                stepRuntime.cancel();
            }
        }
    }

    protected void sendNotifications(Notification.EventType eventType) {
        if (notifications != null && notifications.size() > 0) {
            Transport transport = null;
            Date date = new Date();
            flowParameters.put("_date", DateFormatUtils.format(date, DATE_FORMAT));
            flowParameters.put("_time", DateFormatUtils.format(date, TIME_FORMAT));

            MailSession mailSession = new MailSession(globalSettings);
            try {
                for (Notification notification : notifications) {
                    if (notification.getEventType().equals(eventType.toString())) {
                        log.info("Sending notification '" + notification.getName() + "' of level '"
                                + notification.getNotificationLevel() + "' and type '"
                                + notification.getNotifyType() + "'");
                        transport = mailSession.getTransport();
                        MimeMessage message = new MimeMessage(mailSession.getSession());
                        message.setFrom(new InternetAddress(mailSession.getSession().getProperty(MailSession.SETTING_FROM)));
                        message.setSentDate(new Date());
                        message.setRecipients(RecipientType.BCC, notification.getRecipients());
                        message.setSubject(FormatUtils.replaceTokens(notification.getSubject(),
                                flowParameters, true));
                        message.setText(FormatUtils.replaceTokens(notification.getMessage(),
                                flowParameters, true));
                        try {
                            if (message.getAllRecipients() != null) {
                                transport.sendMessage(message, message.getAllRecipients());
                            }
                        } catch (MessagingException e) {
                            log.error("Failure while sending notification", e);
                        }
                    }
                }
            } catch (MessagingException e) {
                log.error("Failure while preparing notification", e);
            } finally {
                mailSession.closeTransport();
            }
        }
    }

    public ComponentStatistics getComponentStatistics(String flowStepId) {
        return stepRuntimes.get(flowStepId).getComponentContext().getComponentStatistics();
    }

    public String getExecutionId() {
        return executionId;
    }

    public Agent getAgent() {
        return agent;
    }
}
