package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExecutionTracker implements IExecutionTracker {

    final Logger log = LoggerFactory.getLogger(getClass());
    AgentDeployment deployment;

    public ExecutionTracker(AgentDeployment deployment) {
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
    public void beforeHandle(String executionId, ComponentVersion componentVersion) {
        String msg = String.format(
                "started component execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), componentVersion.getComponent().getName(),
                componentVersion.getId());
        log.info(msg);
    }

    @Override
    public void afterHandle(String executionId, ComponentVersion componentVersion) {
        String msg = String.format(
                "finished component execution: %s,  for deployment: %s,  component: %s:%s",
                executionId, deployment.getId(), componentVersion.getComponent().getName(),
                componentVersion.getId());
        log.info(msg);
    }

    @Override
    public void log(String executionId, LogLevel level, ComponentVersion componentVersion,
            String category, String output) {
        String msg = String
                .format("log output from execution: %s, for deployment: %s,  component: %s:%s,  category: %s,  output: %s",
                        executionId, deployment.getId(), componentVersion.getComponent().getName(),
                        componentVersion.getId(), category, output);
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

