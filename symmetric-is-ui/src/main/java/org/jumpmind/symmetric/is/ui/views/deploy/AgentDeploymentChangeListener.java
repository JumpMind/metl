package org.jumpmind.symmetric.is.ui.views.deploy;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;

public interface AgentDeploymentChangeListener {

    public void changed(AgentDeployment agentDeployment);

}
