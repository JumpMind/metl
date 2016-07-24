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
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.component.definition.IComponentDefinitionFactory;
import org.jumpmind.metl.core.runtime.resource.IResourceFactory;
import org.jumpmind.metl.ui.init.BackgroundRefresherService;
import org.jumpmind.metl.ui.views.IUIFactory;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

@UiComponent
@Scope(value = "ui")
public class ApplicationContext implements Serializable {

    private static final long serialVersionUID = 1L;
    
    @Autowired
    IPluginManager pluginManager;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IImportExportService importExportService;
    
    @Autowired
    IExecutionService executionService;

    @Autowired
    IComponentRuntimeFactory componentRuntimeFactory;
    
    @Autowired
    IComponentDefinitionFactory componentDefinitionFactory;
    
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
    String configDir;

    User user = new User();
    
    boolean showRunDiagram = true;
    
    List<ProjectVersion> openProjects = new ArrayList<ProjectVersion>();

    public IConfigurationService getConfigurationService() {
        return configurationService;
    }
    
    public IImportExportService getImportExportService() {
        return importExportService;
    }
    
    public String getConfigDir() {
        return configDir;
    }

    public IExecutionService getExecutionService() {
        return executionService;
    }

    public IComponentRuntimeFactory getComponentRuntimeFactory() {
        return componentRuntimeFactory;
    }
    
    public IComponentDefinitionFactory getComponentDefinitionFactory() {
        return componentDefinitionFactory;
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
        
    public IUIFactory getUiFactory() {
        return uiFactory;
    }
    
    public IPluginManager getPluginManager() {
        return pluginManager;
    }
    
    public void setShowRunDiagram(boolean showRunDiagram) {
        this.showRunDiagram = showRunDiagram;
    }
    
    public boolean isReadOnly(ProjectVersion projectVersion, Privilege privilege) {
        boolean readOnly = projectVersion.isReadOnly();
        if (!readOnly) {
            List<Group> groups = user.getGroups();
            for (Group group : groups) {
                if (group.hasPrivilege(privilege) && !group.isReadOnly()) {
                    readOnly = false;
                    break;
                } else {
                    readOnly = true;
                }
            }
        }
        return readOnly;
    }
}
