package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionTrackerLogger implements IExecutionTracker {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    AgentDeployment deployment;
    
    String executionId;

    public ExecutionTrackerLogger(AgentDeployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public void beforeFlow(String executionId) {
        this.executionId = executionId;
        String msg = String.format("started execution: %s,  for deployment: %s", executionId,
                deployment.getId());
        log.info(msg);
    }

    @Override
    public void afterFlow() {
        String msg = String.format("finished execution: %s,  for deployment: %s", executionId,
                deployment.getId());
        log.info(msg);
    }

    @Override
    public void beforeHandle(ComponentContext context) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "started handing step message for execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), flowStep.getComponent().getName(), flowStep
                        .getComponent().getId());
        log.debug(msg);
    }

    @Override
    public void afterHandle(ComponentContext context, Throwable error) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "finished handing step message for execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), flowStep.getComponent().getName(), flowStep
                        .getComponent().getId());
        log.debug(msg);
    }
    
    @Override
    public void flowStepStarted(ComponentContext context) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "step started for execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), flowStep.getComponent().getName(), flowStep
                        .getComponent().getId());
        log.info(msg);
    }
    
    @Override
    public void flowStepFinished(ComponentContext context, Throwable error, boolean cancelled) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "step completed for execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), flowStep.getComponent().getName(), flowStep
                        .getComponent().getId());
        log.info(msg);
    }
    
    @Override
    public void flowStepFailedOnComplete(ComponentContext context, Throwable error) {
    }

    @Override
    public void log(LogLevel level, ComponentContext context, String output, Object...args) {
        if (deployment.asLogLevel().log(level)) {
            String msg = String
                    .format("log output from execution: %s, for deployment: %s,  component: %s:%s,  output: %s",
                            executionId, deployment.getId(), context.getFlowStep().getComponent()
                                    .getName(), context.getFlowStep().getComponent().getId(),
                            output);
            switch (level) {
                case DEBUG:
                    log.debug(msg);
                    break;
                case INFO:
                    log.info(msg);
                    break;
                case WARN:
                    log.warn(msg);
                    break;
                case ERROR:
                default:
                    log.error(msg);
                    break;
            }
        }
    }
    
    @Override
    public String getExecutionId() {
        return executionId;
    }
}
