package org.jumpmind.symmetric.is.core.runtime;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStatus;
import org.jumpmind.symmetric.is.core.model.ExecutionStep;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentStatistics;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.jumpmind.symmetric.is.core.runtime.flow.AsyncRecorder;

public class ExecutionTrackerRecorder extends ExecutionTrackerLogger {

    AsyncRecorder recorder;

    Execution execution;

    Agent agent;

    Map<String, ExecutionStep> steps;

    public ExecutionTrackerRecorder(Agent agent, AgentDeployment agentDeployment,
            AsyncRecorder recorder) {
        super(agentDeployment);
        this.recorder = recorder;
        this.agent = agent;
    }

    @Override
    public void beforeFlow(String executionId) {
        super.beforeFlow(executionId);
        this.steps = new HashMap<String, ExecutionStep>();
        execution = new Execution();
        execution.setId(executionId);
        execution.setStartTime(new Date());
        execution.setStatus(ExecutionStatus.RUNNING.name());
        execution.setAgentId(deployment.getAgentId());
        execution.setFlowId(deployment.getFlowId());
        execution.setAgentName(agent.getName());
        execution.setFlowName(deployment.getFlow().getName());
        this.recorder.record(execution);
    }

    @Override
    public void afterFlow(String executionId) {
        super.afterFlow(executionId);
        execution.setEndTime(new Date());
        ExecutionStatus status = ExecutionStatus.DONE;
        for (ExecutionStep executionStep : steps.values()) {
            if (ExecutionStatus.ERROR.name().equals(executionStep.getStatus())) {
                status = ExecutionStatus.ERROR;
            }
        }        
        execution.setStatus(status.name());
        execution.setLastUpdateTime(new Date());
        this.recorder.record(execution);
    }
    
    @Override
    public void flowStepStarted(String executionId, IComponent component) {
        super.flowStepStarted(executionId, component);
        ExecutionStep step = steps.get(component.getFlowStep().getId());
        if (step == null) {
            step = new ExecutionStep();
            this.steps.put(component.getFlowStep().getId(), step);
        }
        step.setExecutionId(executionId);
        step.setStartTime(new Date());
        step.setComponentName(component.getFlowStep().getComponent().getName());
        step.setFlowStepId(component.getFlowStep().getId());
        step.setStatus(ExecutionStatus.RUNNING.name());
        this.recorder.record(step);
    }

    @Override
    public void beforeHandle(String executionId, IComponent component) {
        super.beforeHandle(executionId, component);
        ExecutionStep step = steps.get(component.getFlowStep().getId());
        step.setLastUpdateTime(new Date());
        this.recorder.record(step);
    }
 
    @Override
    public void afterHandle(String executionId, IComponent component, Throwable error) {
        super.afterHandle(executionId, component, error);
        ExecutionStep step = steps.get(component.getFlowStep().getId());
        step.setStatus(error != null ? ExecutionStatus.ERROR.name() : ExecutionStatus.RUNNING.name());
        ComponentStatistics stats = component.getComponentStatistics();
        if (stats != null) {
            step.setEntitiesProcessed(stats.getNumberEntitiesProcessed());
            step.setMessagesReceived(stats.getNumberInboundMessages());
            step.setMessagesProduced(stats.getNumberOutboundMessages());
        }
        this.recorder.record(step);
    }
    
    @Override
    public void flowStepFinished(String executionId, IComponent component, Throwable error) {
        super.flowStepFinished(executionId, component, error);
        ExecutionStep step = steps.get(component.getFlowStep().getId());
        step.setEndTime(new Date());
        step.setStatus(error != null ? ExecutionStatus.ERROR.name() : ExecutionStatus.DONE.name());
        if (component.getComponentStatistics() != null) {
            step.setMessagesReceived(component.getComponentStatistics().getNumberInboundMessages());
            step.setMessagesProduced(component.getComponentStatistics().getNumberOutboundMessages());
        }
        this.recorder.record(step);
    }

    @Override
    public void log(String executionId, LogLevel level, IComponent component, String output) {
        super.log(executionId, level, component, output);

        if (deployment.asLogLevel().log(level)) {
            ExecutionStepLog log = new ExecutionStepLog();
            log.setExecutionStepId(steps.get(component.getFlowStep().getId()).getId());
            log.setLevel(level.name());
            log.setLogText(output);
            this.recorder.record(log);
        }
    }

}
