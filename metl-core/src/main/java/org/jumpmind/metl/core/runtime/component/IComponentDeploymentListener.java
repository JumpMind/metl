package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentProjectVersionFlowDeployment;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;

public interface IComponentDeploymentListener {

    public void onDeploy(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep, XMLComponentDefinition componentDefinition);
    
    public void onUndeploy(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep, XMLComponentDefinition componentDefinition);
    
}
