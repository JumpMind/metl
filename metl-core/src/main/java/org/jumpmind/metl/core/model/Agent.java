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
package org.jumpmind.metl.core.model;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.metl.core.plugin.XMLResourceDefinition;
import org.jumpmind.properties.TypedProperties;

public class Agent extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;
    
    public static final String DESIGN_FOLDER_NAME = "<Design Time>";

    Folder folder;

    String name;

    boolean allowTestFlows = false;

    String status = AgentStatus.STOPPED.name();

    Date lastStartTime;

    Date heartbeatTime;

    boolean autoRefresh = true;

    List<AgentDeploy> agentDeployments;

    List<AgentResourceSetting> agentResourceSettings;

    List<AgentParameter> agentParameters;

    boolean deleted;
    
    boolean showResourcesInExploreView;
    
    int execThreadCount = 10;

    public Agent(String name) {
        this();
        this.name = name;
    }

    public Agent() {
        this.agentDeployments = new ArrayList<>();
        this.agentParameters = new ArrayList<>();
    }

    public Agent(Folder folder) {
        this();
        setFolder(folder);
    }
    
    public void setShowResourcesInExploreView(boolean showResourcesOnSupportScreen) {
        this.showResourcesInExploreView = showResourcesOnSupportScreen;
    }
    
    public boolean isShowResourcesInExploreView() {
        return showResourcesInExploreView;
    }

    public void setFolder(Folder folder) {
        this.folder = folder;
    }

    public AgentStatus getAgentStatus() {
        return status == null ? AgentStatus.UNKNOWN : AgentStatus.valueOf(status);
    }

    public void setAgentStatus(AgentStatus agentStatus) {
        status = agentStatus.name();
    }

    public Folder getFolder() {
        return folder;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public void setFolderId(String folderId) {
        if (folderId != null) {
            folder = new Folder(folderId);
        } else {
            folder = null;
        }
    }

    public String getFolderId() {
        return folder != null ? folder.getId() : null;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getLastStartTime() {
        return lastStartTime;
    }

    public void setLastStartTime(Date lastStartTime) {
        this.lastStartTime = lastStartTime;
    }

    public Date getHeartbeatTime() {
        return heartbeatTime;
    }

    public void setHeartbeatTime(Date heartbeatTime) {
        this.heartbeatTime = heartbeatTime;
    }

    public List<AgentDeploy> getAgentDeployments() {
        return agentDeployments;
    }

    public void setAgentDeployments(List<AgentDeploy> agentDeployments) {
        this.agentDeployments = agentDeployments;
    }

    public List<AgentResourceSetting> getAgentResourceSettings() {
        return agentResourceSettings;
    }

    public void setAgentResourceSettings(List<AgentResourceSetting> agentResourceSettings) {
        this.agentResourceSettings = agentResourceSettings;
    }

    public boolean isDeployed(Flow flow) {
        return getAgentDeploymentFor(flow) != null;
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setAllowTestFlows(boolean allowTestFlows) {
        this.allowTestFlows = allowTestFlows;
    }

    public boolean isAllowTestFlows() {
        return allowTestFlows;
    }
    
    public boolean isDesignTimeAgent() {
        return folder != null && DESIGN_FOLDER_NAME.equals(folder.getName());
    }

    public AgentDeploy getAgentDeploymentFor(Flow flow) {
        for (AgentDeploy agentDeployment : agentDeployments) {
            if (isNotBlank(agentDeployment.getFlowId()) && agentDeployment.getFlowId().equals(flow.getId())) {
                return agentDeployment;
            }
        }
        return null;
    }

    public TypedProperties toTypedProperties(XMLResourceDefinition defintion, Resource resource) {
        TypedProperties properties = resource.toTypedProperties(defintion.getSettings().getSetting());
        if (agentResourceSettings != null) {
            for (AgentResourceSetting setting : agentResourceSettings) {
                if (setting.getResourceId().equals(resource.getId())) {
                    properties.setProperty(setting.getName(), setting.getValue());
                }
            }
        }
        return properties;
    }

    public List<AgentParameter> getAgentParameters() {
        return agentParameters;
    }

    public void setAgentParameters(List<AgentParameter> agentParameters) {
        this.agentParameters = agentParameters;
    }

    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    public boolean isAutoRefresh() {
        return autoRefresh;
    }
    
    public void setExecThreadCount(int execThreadCount) {
        this.execThreadCount = execThreadCount;
    }
    
    public int getExecThreadCount() {
        return execThreadCount;
    }

}
