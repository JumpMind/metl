package org.jumpmind.symmetric.is.core.runtime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.AgentStatus;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.DeploymentStatus;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentEngine {

    final Logger log = LoggerFactory.getLogger(getClass());

    Agent agent;

    boolean started = false;

    boolean starting = false;

    boolean stopping = false;

    Map<AgentDeployment, AgentDeploymentRuntime> coordinators = new HashMap<AgentDeployment, AgentDeploymentRuntime>();

    IConfigurationService configurationService;

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    public AgentEngine(Agent agent, IConfigurationService configurationService,
            IComponentFactory componentFactory, IConnectionFactory connectionFactory) {
        this.agent = agent;
        this.configurationService = configurationService;
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;
    }

    public void setAgent(Agent agent) {
        this.agent = agent;
    }

    public synchronized void start() {
        if (!started && !starting) {
            starting = true;
            log.info("Agent '{}' is being started", agent);
            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(
                    agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                deploy(deployment);
            }
            agent.setAgentStatus(AgentStatus.RUNNING);
            configurationService.save(agent);
            started = true;
            starting = false;
            log.info("Agent '{}' has been started", agent);
        }
    }

    public synchronized void stop() {
        if (started && !stopping) {
            stopping = true;
            List<AgentDeployment> deployments = new ArrayList<AgentDeployment>(agent.getAgentDeployments());
            for (AgentDeployment deployment : deployments) {
                undeploy(deployment);
            }

            agent.setAgentStatus(AgentStatus.STOPPED);
            configurationService.save(agent);
            started = false;
            stopping = false;
            log.info("Agent '{}' has been stopped", agent);
        }
    }

    public boolean isStarted() {
        return started;
    }

    public void deploy(AgentDeployment deployment) {
        try {
            List<AgentDeployment> deployments = agent.getAgentDeployments();
            deployments.remove(deployment);
            deployments.add(deployment);
            AgentDeploymentRuntime coordinator = new AgentDeploymentRuntime(deployment,
                    componentFactory, connectionFactory, new ExecutionTracker(deployment));
            coordinators.put(deployment, coordinator);
            coordinator.start();
            deployment.getData().setStatus(DeploymentStatus.RUNNING.name());
            deployment.getData().setMessage("");
            log.info("Flow '{}' has been deployed", deployment.getComponentFlowVersion().getName());
        } catch (Exception e) {
            log.warn("Failed to start '{}'", deployment.getComponentFlowVersion().getName(), e);
            deployment.getData().setStatus(DeploymentStatus.ERROR.name());
            deployment.getData().setMessage(ExceptionUtils.getRootCauseMessage(e));
        }
        configurationService.save(deployment.getData());
    }

    public void undeploy(AgentDeployment deployment) {
        agent.getAgentDeployments().remove(deployment);
        AgentDeploymentRuntime coordinator = coordinators.get(deployment);
        if (coordinator != null) {
            try {
                coordinator.stop();
                log.info("Flow '{}' has been undeployed", deployment.getComponentFlowVersion()
                        .getName());
                deployment.getData().setStatus(DeploymentStatus.STOPPED.name());
                deployment.getData().setMessage("");
                configurationService.save(deployment.getData());
            } catch (Exception e) {
                log.warn("Failed to stop '{}'", deployment.getComponentFlowVersion().getName(), e);
            }
        }
    }

    protected AgentDeploymentRuntime getComponentFlowCoordinator(AgentDeployment deployment) {
        return coordinators.get(deployment);
    }

    class ExecutionTracker implements IExecutionTracker {

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
                    executionId, deployment.getId(), componentVersion.getName(),
                    componentVersion.getId());
            log.info(msg);
        }

        @Override
        public void afterHandle(String executionId, ComponentVersion componentVersion) {
            String msg = String.format(
                    "finished component execution: %s,  for deployment: %s,  component: %s:%s",
                    executionId, deployment.getId(), componentVersion.getName(),
                    componentVersion.getId());
            log.info(msg);
        }

        @Override
        public void log(String executionId, LogLevel level, ComponentVersion componentVersion,
                String category, String output) {
            String msg = String
                    .format("log output from execution: %s, for deployment: %s,  component: %s:%s,  category: %s,  output: %s",
                            executionId, deployment.getId(), componentVersion.getName(),
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

}
