package org.jumpmind.symmetric.is.core.runtime.flow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentStatistics;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.IResource;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.util.AppUtils;

public class FlowRuntime {

    AgentDeployment deployment;

    Map<FlowStep, IComponent> endpointRuntimes = new HashMap<FlowStep, IComponent>();

    Map<String, IResource> resourceRuntimes = new HashMap<String, IResource>();

    IComponentFactory componentFactory;

    IResourceFactory resourceFactory;

    IExecutionTracker executionTracker;

    ExecutorService threadService;

    Map<String, StepRuntime> stepRuntimes;
    
    String executionId;
    
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

    public void start(String executionId) throws InterruptedException {
        
        this.executionId = executionId == null ? UUID.randomUUID().toString() : executionId;
        this.stepRuntimes = new HashMap<String, StepRuntime>();
        Flow flow = deployment.getFlow();
        List<FlowStep> steps = flow.getFlowSteps();
        
        executionTracker.beforeFlow(executionId);

        /* create a step runtime for every component in the flow */
        for (FlowStep flowStep : steps) {
            StepRuntime stepRuntime = new StepRuntime(executionId, componentFactory.create(flowStep, flow), executionTracker);
            stepRuntimes.put(flowStep.getId(), stepRuntime);
        }

        List<FlowStepLink> links = flow.getFlowStepLinks();

        /* for each step runtime, set their list of target step runtimes */
        for (String stepId : stepRuntimes.keySet()) {
            List<StepRuntime> targetStepRuntimes = new ArrayList<StepRuntime>();
            List<StepRuntime> sourceStepRuntimes = new ArrayList<StepRuntime>();
            for (FlowStepLink flowStepLink : links) {
                if (stepId.equals(flowStepLink.getSourceStepId())) {
                    targetStepRuntimes.add(stepRuntimes.get(flowStepLink
                            .getTargetStepId()));
                } else if (stepId.equals(flowStepLink.getTargetStepId())) {
                    sourceStepRuntimes.add(stepRuntimes.get(flowStepLink
                            .getSourceStepId()));
                }
            }
            StepRuntime runtime = stepRuntimes.get(stepId);
            runtime.setTargetStepRuntimes(targetStepRuntimes);
            runtime.setSourceStepRuntimes(sourceStepRuntimes);
        }

        List<StepRuntime> startSteps = findStartSteps();

        /* each step is started as a thread */
        for (StepRuntime stepRuntime : stepRuntimes.values()) {
            threadService.execute(stepRuntime);
        }

        /* start up each step runtime */
        for (StepRuntime stepRuntime : stepRuntimes.values()) {
            stepRuntime.start(executionTracker, resourceFactory);
        }

        StartupMessage startMessage = new StartupMessage();
        // TODO set parameters on start message
        /*
         * for each start step (step that has no input msgs), send a start
         * message to that step
         */
        for (StepRuntime stepRuntime : startSteps) {
            stepRuntime.put(startMessage);
        }
    }

    /*
     * Waiting until all steps have exited
     */
    public void waitForFlowCompletion() throws InterruptedException {
        while (isRunning()) {
            AppUtils.sleep(500);
        }
        
        executionTracker.afterFlow(executionId);
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
                if (stepId.equals(flowStepLink.getTargetStepId())) {
                    isTargetStep = true;
                }
            }

            if (!isTargetStep) {
                starterSteps.add(stepRuntimes.get(stepId));
            }
        }
        return starterSteps;
    }

    public void stop() throws InterruptedException {
        if (isRunning()) {
            List<StepRuntime> startSteps = findStartSteps();
            for (StepRuntime stepRuntime : startSteps) {
                stepRuntime.put(new ShutdownMessage(stepRuntime.getComponent()
                        .getFlowStep().getId()));
            }
        }
    }

    public ComponentStatistics getComponentStatistics(String flowStepId) {
        return stepRuntimes.get(flowStepId).getComponent().getComponentStatistics();
    }
}
