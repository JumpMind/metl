package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.runtime.component.IComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionTrackerLogger implements IExecutionTracker {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    AgentDeployment deployment;

    public ExecutionTrackerLogger(AgentDeployment deployment) {
        this.deployment = deployment;
    }

    @Override
    public void beforeFlow(String executionId) {
        String msg = String.format("started execution: %s,  for deployment: %s", executionId,
                deployment.getId());
        log.info(msg);
    }

    @Override
    public void afterFlow(String executionId) {
        String msg = String.format("finished execution: %s,  for deployment: %s", executionId,
                deployment.getId());
        log.info(msg);
    }

    @Override
    public void beforeHandle(String executionId, IComponent component) {
        FlowStep flowStep = component.getFlowStep();
        String msg = String.format(
                "started handing step message for execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), flowStep.getComponent().getName(), flowStep
                        .getComponent().getId());
        log.debug(msg);
    }

    @Override
    public void afterHandle(String executionId, IComponent component, Throwable error) {
        FlowStep flowStep = component.getFlowStep();
        String msg = String.format(
                "finished handing step message for execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), flowStep.getComponent().getName(), flowStep
                        .getComponent().getId());
        log.debug(msg);
    }
    
    @Override
    public void flowStepStarted(String executionId, IComponent component) {
        FlowStep flowStep = component.getFlowStep();
        String msg = String.format(
                "step started for execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), flowStep.getComponent().getName(), flowStep
                        .getComponent().getId());
        log.info(msg);
    }
    
    @Override
    public void flowStepFinished(String executionId, IComponent component, Throwable error, boolean cancelled) {
        FlowStep flowStep = component.getFlowStep();
        String msg = String.format(
                "step completed for execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), flowStep.getComponent().getName(), flowStep
                        .getComponent().getId());
        log.info(msg);
    }

    @Override
    public void log(String executionId, LogLevel level, IComponent component, String output) {
        if (deployment.asLogLevel().log(level)) {
            String msg = String
                    .format("log output from execution: %s, for deployment: %s,  component: %s:%s,  output: %s",
                            executionId, deployment.getId(), component.getFlowStep().getComponent()
                                    .getName(), component.getFlowStep().getComponent().getId(),
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
}
