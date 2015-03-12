package org.jumpmind.symmetric.is.core.runtime;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Execution;
import org.jumpmind.symmetric.is.core.model.ExecutionStatus;
import org.jumpmind.symmetric.is.core.model.ExecutionStep;
import org.jumpmind.symmetric.is.core.model.ExecutionStepLog;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.jumpmind.symmetric.is.core.runtime.flow.AsyncRecorder;

public class ExecutionTrackerRecorder extends ExecutionTrackerLogger {

    AsyncRecorder recorder;
    
    Execution execution;
    
    Map<String, ExecutionStep> steps;
    
    public ExecutionTrackerRecorder(AgentDeployment deployment, AsyncRecorder recorder) {
        super(deployment);
        this.recorder = recorder;        
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
        execution.setFlowVersionId(deployment.getFlowVersionId());
        //execution.setAgentName();
        execution.setFlowName(deployment.getFlowVersion().getFlow().getName());
        this.recorder.record(execution);
    }

    @Override
    public void afterFlow(String executionId) {
        super.afterFlow(executionId);
        execution.setEndTime(new Date());
        execution.setStatus(ExecutionStatus.DONE.name());
        execution.setLastModifyTime(new Date());
        this.recorder.record(execution);
    }

    @Override
    public void beforeHandle(String executionId, IComponent component) {
        super.beforeHandle(executionId, component);
        ExecutionStep step = new ExecutionStep();
        step.setExecutionId(executionId);
        step.setStartTime(new Date());
        step.setComponentName(component.getFlowStep().getComponentVersion().getComponent().getName());
        step.setFlowStepId(component.getFlowStep().getId());
        step.setStatus(ExecutionStatus.RUNNING.name());
        this.steps.put(component.getFlowStep().getId(), step);
        this.recorder.record(step);
    }

    @Override
    public void afterHandle(String executionId, IComponent component, Throwable error) {
        super.afterHandle(executionId, component, error);
        ExecutionStep step = steps.get(component.getFlowStep().getId());
        step.setEndTime(new Date());
        step.setStatus(error != null ? ExecutionStatus.ERROR.name() : ExecutionStatus.DONE.name());
        step.setMessagesReceived(component.getComponentStatistics().getNumberInboundMessages());
        step.setMessagesProduced(component.getComponentStatistics().getNumberOutboundMessages());

        this.recorder.record(step);
    }

    @Override
    public void log(String executionId, LogLevel level, IComponent component,
            String output) {
        super.log(executionId, level, component, output);
        ExecutionStepLog log = new ExecutionStepLog();
        log.setExecutionStepId(steps.get(component.getFlowStep().getId()).getId());
        log.setLevel(level.name());
        log.setLogText(output);
        this.recorder.record(log);
    }

    
    
}
