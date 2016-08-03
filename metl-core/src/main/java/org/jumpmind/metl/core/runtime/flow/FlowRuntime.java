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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.AgentDeploymentParameter;
import org.jumpmind.metl.core.model.AgentParameter;
import org.jumpmind.metl.core.model.EntityRow;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.ExecutionTrackerLogger;
import org.jumpmind.metl.core.runtime.ExecutionTrackerRecorder;
import org.jumpmind.metl.core.runtime.IExecutionTracker;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.core.runtime.component.ComponentContext;
import org.jumpmind.metl.core.runtime.component.ComponentStatistics;
import org.jumpmind.metl.core.runtime.component.IComponentRuntime;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.EntityResult;
import org.jumpmind.metl.core.runtime.component.definition.IComponentDefinitionFactory;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.util.MailSession;
import org.jumpmind.util.AppUtils;
import org.jumpmind.util.FormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowRuntime {
    
    final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final String DATE_FORMAT = "yyyy-MM-dd";
    
    private static final String TIME_FORMAT = "HH:mm:ss";

    AgentDeployment deployment;

    Map<FlowStep, IComponentRuntime> endpointRuntimes = new HashMap<FlowStep, IComponentRuntime>();

    Map<String, IResourceRuntime> resourceRuntimes = new HashMap<String, IResourceRuntime>();

    IComponentRuntimeFactory componentRuntimeFactory;
    
    IComponentDefinitionFactory componentDefinitionFactory;

    IResourceFactory resourceFactory;

    IExecutionTracker executionTracker;

    ExecutorService threadService;

    Map<String, StepRuntime> stepRuntimes;
    
    Agent agent;
    
    Map<String, String> flowParameters;
    
    List<Notification> notifications;
    
    MailSession mailSession;
    
    IConfigurationService configurationService;
    
    IExecutionService executionService;    
    
    public FlowRuntime(AgentDeployment deployment, IComponentRuntimeFactory componentRuntimeFactory, IComponentDefinitionFactory componentDefinitionFactory,
            IResourceFactory resourceFactory, 
            ExecutorService threadService, IConfigurationService configurationService, IExecutionService executionService) {
        this.deployment = deployment;
        this.componentRuntimeFactory = componentRuntimeFactory;
        this.componentDefinitionFactory = componentDefinitionFactory;
        this.resourceFactory = resourceFactory;
        this.threadService = threadService;
        this.configurationService = configurationService;
        this.executionService = executionService;
    }
    
    public AgentDeployment getDeployment() {
        return deployment;
    }
    
    public ArrayList<EntityRow> getEntityResult() {
        ArrayList<EntityRow> response = null;
        Collection<StepRuntime> steps = stepRuntimes.values();
        for (StepRuntime stepRuntime : steps) {
            List<IComponentRuntime> runtimes = stepRuntime.getComponentRuntimes();
            for (IComponentRuntime runtime : runtimes) {
                if (runtime instanceof EntityResult) {
                    if (response == null) {
                        response = new ArrayList<>();
                    }
                    response.addAll(((EntityResult)runtime).getResponse());
                }
            }
        }
        return response;
    }
    

    public void start(String executionId, Map<String, IResourceRuntime> deployedResources, Agent agent, List<Notification> notifications,
            Map<String, String> globalSettings) throws InterruptedException {    
    	start(executionId, deployedResources, agent, notifications, globalSettings, null);
    }

    public void start(String executionId, Map<String, IResourceRuntime> deployedResources, Agent agent, List<Notification> notifications,
            Map<String, String> globalSettings, Map<String, String> runtimeParameters) throws InterruptedException {    
        
        if (threadService != null && executionService != null) {
            this.executionTracker = new ExecutionTrackerRecorder(agent, deployment, threadService, executionService);
        } else {
            this.executionTracker = new ExecutionTrackerLogger(deployment);
        }
        this.stepRuntimes = new HashMap<String, StepRuntime>();
        this.agent = agent;
        this.notifications = notifications;
        this.mailSession = new MailSession(globalSettings);
        
        Map<String, String> parameters = getFlowParameters(agent, deployment);
        
        flowParameters = new HashMap<String, String>();
        flowParameters.putAll(parameters);
        if (runtimeParameters != null) {
            flowParameters.putAll(runtimeParameters);
        }
        
        Flow manipulatedFlow = manipulateFlow(deployment.getFlow());

        executionTracker.beforeFlow(executionId);
        sendNotifications(Notification.EventType.FLOW_START);

        /* create a step runtime for every component in the flow */
        for (FlowStep flowStep : manipulatedFlow.getFlowSteps()) {
            boolean enabled = flowStep.getComponent().getBoolean(AbstractComponentRuntime.ENABLED, true);
            if (enabled) {
                ComponentContext context = new ComponentContext(deployment, flowStep, manipulatedFlow, executionTracker, 
                        deployedResources, flowParameters, globalSettings);
                StepRuntime stepRuntime = new StepRuntime(componentRuntimeFactory, componentDefinitionFactory, context, this);
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

        
        List<StepRuntime> startSteps = findStartSteps();
        
        /* start up each step runtime */
        manipulatedFlow.calculateApproximateOrder();
        List<FlowStep> flowSteps = manipulatedFlow.getFlowSteps();
        for (FlowStep flowStep : flowSteps) {
            StepRuntime stepRuntime = stepRuntimes.get(flowStep.getId());
            if (stepRuntime != null) {
                try {
                    stepRuntime.start(resourceFactory);
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
        for (FlowStep flowStep : new ArrayList<>(flow.getFlowSteps())) {
            XMLComponent componentDefintion = componentDefinitionFactory.getDefinition(flowStep.getComponent().getType());
            if (isNotBlank(componentDefintion.getFlowManipulatorClassName())) {
                try {
                    IFlowManipulator flowManipulator = (IFlowManipulator) Class.forName(componentDefintion.getFlowManipulatorClassName())
                            .newInstance();
                    flow = flowManipulator.manipulate(flow, flowStep, configurationService);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return flow;
    }
    
    public static Map<String, String> getFlowParameters(Flow flow, Agent agent, AgentDeployment agentDeployment) {
        Map<String, String> params = new HashMap<String, String>();
        List<FlowParameter> flowParameters = flow.getFlowParameters();
        for (FlowParameter flowParameter : flowParameters) {
            params.put(flowParameter.getName(), flowParameter.getDefaultValue());
        }
        return getFlowParameters(params, agent, agentDeployment);
    }

    public static Map<String, String> getFlowParameters(Agent agent, AgentDeployment agentDeployment) {
        Map<String, String> params = new HashMap<String, String>();
        return getFlowParameters(params, agent, agentDeployment);
    }
    
    public static Map<String, String> getFlowParameters(Map<String, String> params, Agent agent, AgentDeployment agentDeployment) {
        List<AgentDeploymentParameter> deployParameters = agentDeployment.getAgentDeploymentParameters();
        List<AgentParameter> agentParameters = agent.getAgentParameters();
        if (agentParameters != null) {
            for (AgentParameter agentParameter : agentParameters) {
                params.put(agentParameter.getName(), agentParameter.getValue());
            }
        }
        if (deployParameters != null) {
            for (AgentDeploymentParameter deployParameter : deployParameters) {
                params.put(deployParameter.getName(), deployParameter.getValue());
            }
        }
        Date date = new Date();
        params.put("_agentName", agent.getName());
        params.put("_deploymentName", agentDeployment.getName());
        params.put("_flowName", agentDeployment.getFlow().getName());
        params.put("_host", agent.getHost());
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
        
        // Check getAllErrors here to make sure any new errors are trapped from the flowCompleted methods
        if (getAllErrors().size() > 0) {
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
            List<FlowStepLink> links = deployment.getFlow()
                    .getFlowStepLinks();
            boolean isTargetStep = false;
            for (FlowStepLink flowStepLink : links) {
                if (stepRuntimes.get(flowStepLink.getSourceStepId()) != null 
                        && stepId.equals(flowStepLink.getTargetStepId())) {
                    isTargetStep = true;
                }
            }

            if (!isTargetStep) {
                starterSteps.add(stepRuntimes.get(stepId));
            }
        }
        return starterSteps;
    }

    public void cancel() {
        if (stepRuntimes != null) {
            for (StepRuntime stepRuntime : stepRuntimes.values()) {
                if (stepRuntime.isRunning()) {
                    try {
                        stepRuntime.inQueue.clear();
                        stepRuntime.queue(new ShutdownMessage(stepRuntime.getComponentContext().getFlowStep().getId(), true));
                    } catch (InterruptedException e) {
                    }
                } else  {
                    stepRuntime.cancel();
                }
            }
        }
    }

    protected void sendNotifications(Notification.EventType eventType) {        
        if (notifications != null && notifications.size() > 0) {                
            Transport transport = null;
            Date date = new Date();
            flowParameters.put("_date", DateFormatUtils.format(date, DATE_FORMAT));
            flowParameters.put("_time", DateFormatUtils.format(date, TIME_FORMAT));

            try {
                for (Notification notification : notifications) {
                    if (notification.getEventType().equals(eventType.toString())) {
                        log.info("Sending notification '" + notification.getName() + "' of level '" + notification.getLevel() +
                                "' and type '" + notification.getNotifyType() + "'");
                        transport = mailSession.getTransport();
                        MimeMessage message = new MimeMessage(mailSession.getSession());
                        message.setSentDate(new Date());
                        message.setRecipients(RecipientType.BCC, notification.getRecipients());
                        message.setSubject(FormatUtils.replaceTokens(notification.getSubject(), flowParameters, true));
                        message.setText(FormatUtils.replaceTokens(notification.getMessage(), flowParameters, true));
                        try {
                            transport.sendMessage(message, message.getAllRecipients());
                        } catch (MessagingException e) {
                            log.error("Failure while sending notification", e);
                        }
                    }
                }
            } catch (MessagingException e) {
                log.error("Failure while preparing notification", e);
            } finally {
                mailSession.closeTransport(transport);
            }
        }
    }

    public ComponentStatistics getComponentStatistics(String flowStepId) {
        return stepRuntimes.get(flowStepId).getComponentContext().getComponentStatistics();
    }
    
    public String getExecutionId() {
        return executionTracker != null ? executionTracker.getExecutionId() : null;
    }
    
    public Agent getAgent() {
        return agent;
    }
}
