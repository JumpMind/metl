package org.jumpmind.symmetric.is.ui.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric.is.core.model.ProjectVersion;
import org.jumpmind.symmetric.is.core.model.User;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.persist.IExecutionService;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.ui.init.BackgroundRefresherService;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;

@UiComponent
@Scope(value = "ui")
public class ApplicationContext implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IExecutionService executionService;

    @Autowired
    IComponentFactory componentFactory;

    @Autowired
    IResourceFactory resourceFactory;

    @Autowired
    BackgroundRefresherService backgroundRefresherService;

    @Autowired
    IAgentManager agentManager;
    
    @Autowired
    IDatabasePlatform configDatabasePlatform;
    
    @Autowired
    Environment environment;

    User user = new User();
    
    List<ProjectVersion> openProjects = new ArrayList<ProjectVersion>();

    public IConfigurationService getConfigurationService() {
        return configurationService;
    }

    public IExecutionService getExecutionService() {
        return executionService;
    }

    public IComponentFactory getComponentFactory() {
        return componentFactory;
    }

    public IResourceFactory getResourceFactory() {
        return resourceFactory;
    }

    public BackgroundRefresherService getBackgroundRefresherService() {
        return backgroundRefresherService;
    }

    public IAgentManager getAgentManager() {
        return agentManager;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }
    
    public List<ProjectVersion> getOpenProjects() {
        return openProjects;
    }

    public IDatabasePlatform getConfigDatabasePlatform() {
        return configDatabasePlatform;
    }
    
    public Environment getEnvironment() {
        return environment;
    }
}
