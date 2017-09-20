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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.jumpmind.metl.core.runtime.LogLevel;

public class AgentDeploymentSummary extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_FLOW = "Flow";
    
    public static final String TYPE_RESOURCE = "Resource";
    
    String projectName;
    
    String artifactId;
    
    String rowId;
    
    String projectVersionLabel;
    
    String type;
    
    String name;

    String status = DeploymentStatus.DISABLED.name();

    String logLevel = LogLevel.DEBUG.name();

    String startType = StartType.MANUAL.name();

    String startExpression;
    
    String url;

    public AgentDeploymentSummary() {
    }

    public void copy(AgentProjectVersionFlowDeployment deployment) {
        AgentDeploy agentDeployment = deployment.getAgentDeployment();
        projectName = deployment.getProjectVersion().getName();
        copy(agentDeployment);
    }
    
    public void copy(AgentDeploy agentDeployment) {
        setId(agentDeployment.getId());
        name = agentDeployment.getName();
        type = TYPE_FLOW;
        status = agentDeployment.getStatus();
        logLevel = agentDeployment.getLogLevel();
        startType = agentDeployment.getStartType();
        startExpression = agentDeployment.getStartExpression();
        artifactId = agentDeployment.getFlowId();
    }
    
    public void setArtifactId(String rowId) {
        this.artifactId = rowId;
    }
    
    public String getArtifactId() {
        return artifactId;
    }

    public String getRowId() {
        return rowId;
    }

    public void setRowId(String rowId) {
        this.rowId = rowId;
    }

    public String getProjectVersionLabel() {
        return projectVersionLabel;
    }

    public void setProjectVersionLabel(String projectVersionLabel) {
        this.projectVersionLabel = projectVersionLabel;
    }

    public boolean isChanged(AgentDeploymentSummary o) {
        return ! new EqualsBuilder().append(getId(), o.getId()).append(projectName, o.projectName).append(type, o.type).append(name, o.name).append(status, o.status)
            .append(logLevel, o.logLevel).append(startType, o.startType).append(startExpression, o.startExpression).isEquals();
    }
        
    public boolean isFlow() {
        return type.equals(TYPE_FLOW);
    }
    
    public boolean isResource() {
        return type.equals(TYPE_RESOURCE);
    }
    
    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = DeploymentStatus.massage(status);
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getStartType() {
        return startType;
    }

    public void setStartType(String startType) {
        this.startType = startType;
    }

    public String getStartExpression() {
        return startExpression;
    }

    public void setStartExpression(String startExpression) {
        this.startExpression = startExpression;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
    
    class DeploymentLine {
        
        String projectName;
        String flowName;
        String newDeployName;
        String newDeployVersion;
        String newDeployType;
        String existingDeployName;
        String existingDeployVersion;
        String existingDeployType;
        boolean undeploy;

        public DeploymentLine(String projectName, String flowName, String newDeployName,
                String newDeployVersion, String newDeployType, String existingDeployName,
                String existingDeployVersion, String existingDeployType, boolean undeploy) {
            
            this.projectName = projectName;
            this.flowName = flowName;
            this.newDeployName = newDeployName;
            this.newDeployVersion = newDeployVersion;
            this.newDeployType = newDeployType;
            this.existingDeployName = existingDeployName;
            this.existingDeployVersion = existingDeployVersion;
            this.existingDeployType = existingDeployType;
            this.undeploy = undeploy;            
        }

        public String getProjectName() {
            return projectName;
        }
        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }
        public String getFlowName() {
            return flowName;
        }
        public void setFlowName(String flowName) {
            this.flowName = flowName;
        }
        public String getNewDeployName() {
            return newDeployName;
        }
        public void setNewDeployName(String newDeployName) {
            this.newDeployName = newDeployName;
        }
        public String getNewDeployVersion() {
            return newDeployVersion;
        }
        public void setNewDeployVersion(String newDeployVersion) {
            this.newDeployVersion = newDeployVersion;
        }
        public String getNewDeployType() {
            return newDeployType;
        }
        public void setNewDeployType(String newDeployType) {
            this.newDeployType = newDeployType;
        }
        public String getExistingDeployName() {
            return existingDeployName;
        }
        public void setExistingDeployName(String existingDeployName) {
            this.existingDeployName = existingDeployName;
        }
        public String getExistingDeployVersion() {
            return existingDeployVersion;
        }
        public void setExistingDeployVersion(String existingDeployVersion) {
            this.existingDeployVersion = existingDeployVersion;
        }
        public String getExistingDeployType() {
            return existingDeployType;
        }
        public void setExistingDeployType(String existingDeployType) {
            this.existingDeployType = existingDeployType;
        }
        public boolean isUndeploy() {
            return undeploy;
        }
        public void setUndeploy(boolean undeploy) {
            this.undeploy = undeploy;
        }        
    }
}
