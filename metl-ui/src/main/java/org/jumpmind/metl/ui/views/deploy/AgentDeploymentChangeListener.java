package org.jumpmind.metl.ui.views.deploy;

import org.jumpmind.metl.core.model.AgentDeployment;

public interface AgentDeploymentChangeListener {

    public void changed(AgentDeployment agentDeployment);

}
