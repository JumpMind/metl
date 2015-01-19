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
import org.jumpmind.symmetric.is.core.config.data.AgentDeploymentData;
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

    Map<Agent, AgentEngine> engines = new HashMap<Agent, AgentEngine>();

    public AgentManager(IConfigurationService configurationService,
            IComponentFactory componentFactory, IConnectionFactory connectionFactory) {
        this.configurationService = configurationService;
        this.componentFactory = componentFactory;
        this.connectionFactory = connectionFactory;
    }

    public void start() {
        Set<Agent> agents = getLocalAgents();
        for (Agent agent : agents) {
            createAndStartEngine(agent);
        }
    }
    
    @Override
    public void undeploy(AgentDeployment agentDeployment) {
        configurationService.delete(agentDeployment);
        AgentEngine engine = getAgentEngine(agentDeployment.getId());
        if (engine != null) {
            engine.undeploy(agentDeployment);
        }
    }
    
    @Override
    public AgentDeployment deploy(String agentId, ComponentFlowVersion componentFlowVersion) {
        AgentDeploymentData data = new AgentDeploymentData();
        data.setAgentId(agentId);
        data.setComponentFlowVersionId(componentFlowVersion.getId());
        AgentDeployment agentDeployment = new AgentDeployment(componentFlowVersion, data);
        configurationService.save(agentDeployment);                    
        AgentEngine engine = getAgentEngine(agentId);
        if (engine != null) {
            engine.deploy(agentDeployment);
        }
        return agentDeployment;
    }

    public Set<Agent> getLocalAgents() {
        Set<Agent> agents = new HashSet<Agent>(configurationService.findAgentsForHost(AppUtils
                .getHostName()));
        agents.addAll(configurationService.findAgentsForHost(AppUtils.getIpAddress()));
        return agents;
    }

    @PreDestroy
    protected void destroy() {
        Collection<AgentEngine> all = engines.values();
        for (AgentEngine agentEngine : all) {
            if (agentEngine.isStarted()) {
                agentEngine.stop();
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

    protected AgentEngine createAndStartEngine(Agent agent) {
        AgentEngine engine = new AgentEngine(agent, configurationService, componentFactory,
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
    public AgentEngine refresh(Agent agent) {
        AgentEngine engine = engines.get(agent);
        if (isAgentLocal(agent)) {
            if (engine == null) {
                engine = createAndStartEngine(agent);
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
        AgentEngine engine = engines.get(agent);
        if (engine != null) {
            engine.stop();
            engines.remove(agent);
        }
    }

    @Override
    public AgentEngine getAgentEngine(Agent agent) {
        return engines.get(agent);
    }

    @Override
    public AgentEngine getAgentEngine(String agentId) {
        Set<Agent> agents = engines.keySet();
        for (Agent agent : agents) {
            if (agent.getId().equals(agentId)) {
                return getAgentEngine(agent);
            }
        }
        return null;
    }

}
