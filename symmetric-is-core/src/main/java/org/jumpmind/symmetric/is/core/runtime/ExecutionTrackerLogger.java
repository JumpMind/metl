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
        String msg = String.format("Flow started for deployment: %s", deployment.getName());
        log.info(msg);
    }

    @Override
    public void afterFlow() {
        String msg = String.format("Flow finished for deployment: %s", deployment.getName());
        log.info(msg);
    }

    @Override
    public void beforeHandle(ComponentContext context) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "Handling message for deployment: %s for component: %s",
                deployment.getName(), flowStep.getName());
        log.debug(msg);
    }

    @Override
    public void afterHandle(ComponentContext context, Throwable error) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "Finished handling message for deployment: %s for component: %s",
                deployment.getName(), flowStep.getName());
        log.debug(msg);
    }
    
    @Override
    public void flowStepStarted(ComponentContext context) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "Started flow step for deployment: %s for component: %s",
                deployment.getName(), flowStep.getName());
        log.info(msg);
    }
    
    @Override
    public void flowStepFinished(ComponentContext context, Throwable error, boolean cancelled) {
        FlowStep flowStep = context.getFlowStep();
        String msg = String.format(
                "Finished flow step for deployment: %s for component: %s",
                deployment.getName(), flowStep.getName());
        log.info(msg);
    }
    
    @Override
    public void flowStepFailedOnComplete(ComponentContext context, Throwable error) {
    }

    @Override
    public void log(LogLevel level, ComponentContext context, String output, Object...args) {
        if (deployment.asLogLevel().log(level)) {
            if (args != null && args.length > 0) {
                output = String.format(output, args);
            }
            switch (level) {
                case DEBUG:
                    log.debug(output);
                    break;
                case INFO:
                    log.info(output);
                    break;
                case WARN:
                    log.warn(output);
                    break;
                case ERROR:
                default:
                    log.error(output);
                    break;
            }
        }
    }
    
    @Override
    public String getExecutionId() {
        return executionId;
    }
}
