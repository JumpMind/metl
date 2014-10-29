package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class AgentEngine {

    final Logger log = LoggerFactory.getLogger(getClass());

    Agent agent;
    
    boolean started = false;

    @Autowired
    IConfigurationService configurationService;

    public AgentEngine(Agent agent, IConfigurationService configurationService) {
        this.agent = agent;
        this.configurationService = configurationService;
    }

    public void start() {
        started = true;
        log.info("Agent '{}' has been started", agent);
    }

    public void stop() {
        started = false;
        log.info("Agent '{}' has been stopped", agent);
    }
    
    public boolean isStarted() {
        return started;
    }

    public void deploy(ComponentFlowVersion componentGraph) {

    }

    public void undeploy(ComponentFlowVersion componentGraph) {

    }

    protected ComponentFlowCoordinator getComponentFlowCoordinator(
            ComponentFlowVersion componentGraph) {
        return null;
    }

}
