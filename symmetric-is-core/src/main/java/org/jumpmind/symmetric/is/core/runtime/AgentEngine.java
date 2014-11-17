package org.jumpmind.symmetric.is.core.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.AgentStatus;
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
            List<AgentDeployment> deployments = agent.getAgentDeployments();
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
            List<AgentDeployment> deployments = agent.getAgentDeployments();
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
            AgentDeploymentRuntime coordinator = new AgentDeploymentRuntime(
                    deployment, componentFactory, connectionFactory);
            coordinators.put(deployment, coordinator);
            coordinator.start();
            log.info("Flow '{}' has been deployed", deployment.getComponentFlowVersion().getName());
        } catch (Exception e) {
            log.warn("Failed to start '{}'", deployment.getComponentFlowVersion().getName(), e);
        }
    }

    public void undeploy(AgentDeployment deployment) {
        AgentDeploymentRuntime coordinator = coordinators.get(deployment);
        if (coordinator != null) {
            try {
                coordinator.stop();
                log.info("Flow '{}' has been undeployed", deployment.getComponentFlowVersion()
                        .getName());
            } catch (Exception e) {
                log.warn("Failed to stop '{}'", deployment.getComponentFlowVersion().getName(), e);
            }
        }
    }

    protected AgentDeploymentRuntime getComponentFlowCoordinator(AgentDeployment deployment) {
        return coordinators.get(deployment);
    }

}
