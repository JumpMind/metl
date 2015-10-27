/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.jumpmind.metl.ui.init.BackgroundRefresherService;
import org.jumpmind.metl.ui.views.IUIFactory;
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
    IComponentRuntimeFactory componentFactory;
    
    @Autowired
    IUIFactory uiFactory;

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
    
    public String getConfigDir() {
        return environment.getProperty("config.dir");
    }

    public IExecutionService getExecutionService() {
        return executionService;
    }

    public IComponentRuntimeFactory getComponentFactory() {
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
    
    public IUIFactory getUiFactory() {
        return uiFactory;
    }
}
