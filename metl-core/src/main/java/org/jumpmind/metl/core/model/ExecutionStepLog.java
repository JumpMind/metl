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

import org.jumpmind.metl.core.runtime.LogLevel;

public class ExecutionStepLog extends AbstractObject implements Comparable<ExecutionStepLog> {

    private static final long serialVersionUID = 1L;

    private String executionStepId;
    
    private String level;
    
    private String logText;
    
    private Date createTime = new Date();

	public String getExecutionStepId() {
		return executionStepId;
	}

	public void setExecutionStepId(String executionStepId) {
		this.executionStepId = executionStepId;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}
	
	public LogLevel getLogLevel() {
	    return level == null ? null : LogLevel.valueOf(level);
	}

	public String getLogText() {
		return logText;
	}

	public void setLogText(String logText) {
		this.logText = logText;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}
	
	@Override
	public int compareTo(ExecutionStepLog o) {
	    if (createTime != null && o.createTime != null) {
	        return createTime.compareTo(o.createTime);
	    } else {
	        return 0;
	    }
	}

}
