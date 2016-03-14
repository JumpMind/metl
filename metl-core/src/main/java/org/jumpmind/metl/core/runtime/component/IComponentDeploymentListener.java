package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;

public interface IComponentDeploymentListener {

    public void onDeploy(AgentDeployment deployment, FlowStep flowStep, XMLComponent componentDefinition);
    
    public void onUndeploy(AgentDeployment deployment, FlowStep flowStep, XMLComponent componentDefinition);
    
}
