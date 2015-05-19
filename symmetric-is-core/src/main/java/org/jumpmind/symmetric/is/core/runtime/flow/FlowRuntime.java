package org.jumpmind.symmetric.is.core.runtime.flow;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentParameter;
import org.jumpmind.symmetric.is.core.model.AgentParameter;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentContext;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentStatistics;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentRuntime;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceRuntime;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FlowRuntime {
    
    final Logger log = LoggerFactory.getLogger(getClass());

    AgentDeployment deployment;

    Map<FlowStep, IComponentRuntime> endpointRuntimes = new HashMap<FlowStep, IComponentRuntime>();

    Map<String, IResourceRuntime> resourceRuntimes = new HashMap<String, IResourceRuntime>();

    IComponentFactory componentFactory;

    IResourceFactory resourceFactory;

    IExecutionTracker executionTracker;

    ExecutorService threadService;

    Map<String, StepRuntime> stepRuntimes;    
    
    public FlowRuntime(AgentDeployment deployment, IComponentFactory componentFactory,
            IResourceFactory resourceFactory, IExecutionTracker executionTracker,
            ExecutorService threadService) {
        this.executionTracker = executionTracker;
        this.deployment = deployment;
        this.componentFactory = componentFactory;
        this.resourceFactory = resourceFactory;
        this.threadService = threadService;
    }
    
    public AgentDeployment getDeployment() {
        return deployment;
    }

    public void start(String executionId, Map<String, IResourceRuntime> deployedResources, List<AgentParameter> agentParameters) 
            throws InterruptedException {
        
        this.stepRuntimes = new HashMap<String, StepRuntime>();
        Flow flow = deployment.getFlow();
        List<FlowStep> steps = flow.getFlowSteps();
        
        executionTracker.beforeFlow(executionId);

        /* create a step runtime for every component in the flow */
        for (FlowStep flowStep : steps) {
            boolean enabled = flowStep.getComponent().getBoolean(AbstractComponentRuntime.ENABLED, true);
            if (enabled) {
                Map<String, Serializable> parameters = getFlowParameters(deployment.getAgentDeploymentParameters(), agentParameters);
                ComponentContext context = new ComponentContext(flowStep, flow, executionTracker, 
                        deployedResources.get(flowStep.getComponent().getResourceId()), parameters);
                StepRuntime stepRuntime = new StepRuntime(componentFactory.create(flowStep.getComponent().getType()), context);
                stepRuntimes.put(flowStep.getId(), stepRuntime);
            }
        }
        
        List<FlowStepLink> links = flow.getFlowStepLinks();

        /* for each step runtime, set their list of target step runtimes */
        for (String stepId : stepRuntimes.keySet()) {
            List<StepRuntime> targetStepRuntimes = new ArrayList<StepRuntime>();
            List<StepRuntime> sourceStepRuntimes = new ArrayList<StepRuntime>();
            for (FlowStepLink flowStepLink : links) {
                if (stepId.equals(flowStepLink.getSourceStepId())) {
                    StepRuntime runtime = stepRuntimes.get(flowStepLink.getTargetStepId());
                    if (runtime != null) {
                        targetStepRuntimes.add(runtime);
                    }
                } else if (stepId.equals(flowStepLink.getTargetStepId())) {
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
        for (StepRuntime stepRuntime : stepRuntimes.values()) {
            try {
                stepRuntime.start(executionTracker, resourceFactory);                
            } catch (RuntimeException ex) {
                stepRuntime.error = ex;
                throw ex;
            }
        }
        
        /* each step is started as a thread */
        for (StepRuntime stepRuntime : stepRuntimes.values()) {
            stepRuntime.setRunning(true);
            threadService.execute(stepRuntime);
        }

        StartupMessage startMessage = new StartupMessage();
        /*
         * for each start step (step that has no input msgs), send a start
         * message to that step
         */
        for (StepRuntime stepRuntime : startSteps) {
            stepRuntime.queue(startMessage);
        }
    }

    protected Map<String, Serializable> getFlowParameters(List<AgentDeploymentParameter> deployParameters, List<AgentParameter> agentParameters) {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        for (AgentParameter agentParameter : agentParameters) {
            params.put(agentParameter.getName(), agentParameter.getValue());
        }
        for (AgentDeploymentParameter deployParameter : deployParameters) {
            params.put(deployParameter.getName(), deployParameter.getValue());
        }
        return params;
    }

    /*
     * Waiting until all steps have exited
     */
    public void waitForFlowCompletion() {
        while (isRunning()) {
            AppUtils.sleep(500);
        }        
    }
    
    public void notifyStepsTheFlowIsComplete() {
        Collection<StepRuntime> allSteps = stepRuntimes.values();
        List<Throwable> allErrors = new ArrayList<Throwable>();
        for (StepRuntime stepRuntime : allSteps) {
            if (stepRuntime.getError() != null) {
               allErrors.add(stepRuntime.getError());
            }
        }

        for (StepRuntime stepRuntime : allSteps) {
            if (allErrors.size() > 0) {
                stepRuntime.flowCompletedWithErrors(stepRuntime.getError(), allErrors);
            } else {
                stepRuntime.flowCompletedWithoutError();
            }
        }
        
        executionTracker.afterFlow();
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

    public void stop() {
        for (StepRuntime stepRuntime : stepRuntimes.values()) {
            if (stepRuntime.isRunning()) {
                try {
                    stepRuntime.queue(new ShutdownMessage(stepRuntime.getComponentRuntime()
                            .getComponentContext().getFlowStep().getId()));
                } catch (InterruptedException e) {
                }
            } else {
                stepRuntime.finished();
            }
        }
    }

    public ComponentStatistics getComponentStatistics(String flowStepId) {
        return stepRuntimes.get(flowStepId).getComponentRuntime().getComponentContext().getComponentStatistics();
    }
    
    public String getExecutionId() {
        return executionTracker.getExecutionId();
    }
}
