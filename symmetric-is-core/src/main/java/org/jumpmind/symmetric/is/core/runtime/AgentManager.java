package org.jumpmind.symmetric.is.core.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.AgentStartMode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentManager implements IAgentManager {

    final Logger log = LoggerFactory.getLogger(getClass());

    IConfigurationService configurationService;

    IComponentFactory componentFactory;

    IConnectionFactory connectionFactory;

    Map<Agent, AgentRuntime> engines = new HashMap<Agent, AgentRuntime>();

    public AgentManager(IConfigurationService configurationService,
            IComponentFactory componentFactory, IConnectionFactory connectionFactory) {
        this.configurationService = configurationService;
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;
    }

    public void start() {
        Set<Agent> agents = getLocalAgents();
        for (Agent agent : agents) {
            createAndStartRuntime(agent);
        }
    }
    
    @Override
    public void undeploy(AgentDeployment agentDeployment) {
        AgentRuntime engine = getAgentRuntime(agentDeployment.getData().getAgentId());
        if (engine != null) {
            engine.undeploy(agentDeployment);
        }
    }
    
    @Override
    public AgentDeployment deploy(String agentId, ComponentFlowVersion componentFlowVersion) {
        AgentDeployment deployment = null;
        AgentRuntime engine = getAgentRuntime(agentId);
        if (engine != null) {
            deployment = engine.deploy(componentFlowVersion);
        }
        return deployment;
    }

    public Set<Agent> getLocalAgents() {
        Set<Agent> agents = new HashSet<Agent>(configurationService.findAgentsForHost(AppUtils
                .getHostName()));
        agents.addAll(configurationService.findAgentsForHost(AppUtils.getIpAddress()));
        return agents;
    }

    @PreDestroy
    protected void destroy() {
        Collection<AgentRuntime> all = engines.values();
        for (AgentRuntime runtime : all) {
            if (runtime.isStarted()) {
                runtime.stop();
            }
        }
    }

    @Override
    public boolean isAgentLocal(Agent agent) {
        String hostName = agent.getData().getHost();
        return "localhost".equals(hostName) || "127.0.0.1".equals(hostName)
                || "::1".equals(hostName) || AppUtils.getHostName().equals(hostName)
                || AppUtils.getIpAddress().equals(hostName);
    }

    protected AgentRuntime createAndStartRuntime(Agent agent) {
        AgentRuntime engine = new AgentRuntime(agent, configurationService, componentFactory,
                connectionFactory);
        engines.put(agent, engine);
        if (agent.getAgentStartMode() == AgentStartMode.AUTO) {
            engine.start();
        } else {
            log.info(
                    "The '{}' agent is configured to be started manually. It will not be auto started.",
                    agent.toString());
        }
        return engine;
    }

    @Override
    public AgentRuntime refresh(Agent agent) {
        AgentRuntime engine = engines.get(agent);
        if (isAgentLocal(agent)) {
            if (engine == null) {
                engine = createAndStartRuntime(agent);
            } else {
                engine.setAgent(agent);
                engine.stop();
                engine.start();
            }
        } else {
            if (engine != null) {
                engine.stop();
                engines.remove(agent);
            }
        }
        return engine;
    }

    @Override
    public void remove(Agent agent) {
        AgentRuntime runtime = engines.get(agent);
        if (runtime != null) {
            runtime.stop();
            engines.remove(agent);
        }
    }

    @Override
    public AgentRuntime getAgentRuntime(Agent agent) {
        return engines.get(agent);
    }

    @Override
    public AgentRuntime getAgentRuntime(String agentId) {
        Set<Agent> agents = engines.keySet();
        for (Agent agent : agents) {
            if (agent.getId().equals(agentId)) {
                return getAgentRuntime(agent);
            }
        }
        return null;
    }

}
