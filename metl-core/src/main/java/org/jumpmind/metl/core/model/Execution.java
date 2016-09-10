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

import java.util.Date;

public class Execution extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String agentId;
    
    String flowId;
    
    String deploymentId;
    
    String deploymentName;
    
    String agentName;
    
    String hostName;
    
    String flowName;

    String status;
    
    String parameters;

    Date startTime;

    Date endTime;

    public Execution() {
    }

    public Execution(ExecutionStatus status) {
        setExecutionStatus(status);
    }
    
    public boolean isDone() {
        ExecutionStatus status = getExecutionStatus();
        return (status == ExecutionStatus.ABANDONED || status == ExecutionStatus.CANCELLED
                || status == ExecutionStatus.ERROR || status == ExecutionStatus.DONE);
    }
    
    public boolean isNotSuccess() {
        ExecutionStatus status = getExecutionStatus();
        return (status == ExecutionStatus.ABANDONED || status == ExecutionStatus.CANCELLED
                || status == ExecutionStatus.ERROR);
    }

    public void setExecutionStatus(ExecutionStatus status) {
        setStatus(status.name());
    }

    public ExecutionStatus getExecutionStatus() {
        return status == null ? null : ExecutionStatus.valueOf(status);
    }

    public void setName(String name) {
    }

    public String getName() {
        return getId();
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

	public String getAgentId() {
		return agentId;
	}

	public void setAgentId(String agentId) {
		this.agentId = agentId;
	}

	public String getFlowId() {
		return flowId;
	}

	public void setFlowId(String flowId) {
		this.flowId = flowId;
	}

	public String getAgentName() {
		return agentName;
	}

	public void setAgentName(String agentName) {
		this.agentName = agentName;
	}

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}
	
	public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }
	
	public String getDeploymentId() {
        return deploymentId;
    }
	
	public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }
	
	public String getDeploymentName() {
        return deploymentName;
    }
	
	public void setParameters(String parameters) {
        this.parameters = parameters;
    }
	
	public String getParameters() {
        return parameters;
    }

}
