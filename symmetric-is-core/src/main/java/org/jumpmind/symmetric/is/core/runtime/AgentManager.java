package org.jumpmind.symmetric.is.core.runtime;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentStartMode;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentManager implements IAgentManager {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    IConfigurationService configurationService;
    
    Map<Agent, AgentEngine> engines = new HashMap<Agent, AgentEngine>();

    public AgentManager(IConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @PostConstruct
    protected void init() {
        Set<Agent> agents = new HashSet<Agent>(configurationService.findAgentsForHost(AppUtils
                .getHostName()));
        agents.addAll(configurationService.findAgentsForHost(AppUtils.getIpAddress()));
        for (Agent agent : agents) {
            AgentEngine engine = new AgentEngine(agent, configurationService);
            engines.put(agent, engine);
            if (agent.getAgentStartMode() == AgentStartMode.AUTO) {
                engine.start();                
            }
        }
    }
    
    @PreDestroy
    protected void destroy () {
        Collection<AgentEngine> all = engines.values();
        for (AgentEngine agentEngine : all) {
            if (agentEngine.isStarted()) {
                agentEngine.stop();
            }
        }
    }

    @Override
    public AgentEngine refresh(Agent agent) {
        return null;
    }

    @Override
    public void remove(Agent agent) {
    }

    @Override
    public AgentEngine getAgentEngine(Agent agent) {
        return null;
    }

}
