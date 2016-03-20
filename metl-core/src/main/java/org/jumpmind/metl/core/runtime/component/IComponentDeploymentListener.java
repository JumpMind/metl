package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;

public interface IComponentDeploymentListener {

    public void onDeploy(Agent agent, AgentDeployment deployment, Flow flow, FlowStep flowStep, XMLComponent componentDefinition);
    
    public void onUndeploy(Agent agent, AgentDeployment deployment, Flow flow, FlowStep flowStep, XMLComponent componentDefinition);
    
}
