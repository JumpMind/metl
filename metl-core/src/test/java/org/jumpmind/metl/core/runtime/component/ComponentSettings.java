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
package org.jumpmind.metl.core.runtime.component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.metl.core.model.FlowStep;

public class ComponentSettings {
	Map<String, String> flowParametersAsString;
	Map<String, Serializable> flowParameters;
	FlowStep flowStep;
	
	public ComponentSettings() {
		this.flowParametersAsString = new HashMap<String, String>();
		this.flowParameters = new HashMap<String, Serializable>();
		this.flowStep = new FlowStep();
	}
	
	Map<String, String> getFlowParametersAsString() {
		return flowParametersAsString;
	}

	void setFlowParametersAsString(Map<String, String> flowParametersAsString) {
		this.flowParametersAsString = flowParametersAsString;
	}

	Map<String, Serializable> getFlowParameters() {
		return flowParameters;
	}

	void setFlowParameters(Map<String, Serializable> flowParameters) {
		this.flowParameters = flowParameters;
	}

	FlowStep getFlowStep() {
		return flowStep;
	}

	void setFlowStep(FlowStep flowStep) {
		this.flowStep = flowStep;
	}
	
	
	
	
	
	
}
