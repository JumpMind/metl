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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.runtime.IExecutionTracker;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class ComponentContext {

    AgentDeploy deployment;

    FlowStep flowStep;

    Flow manipulatedFlow;

    IExecutionTracker executionTracker;

    Map<String, IResourceRuntime> deployedResources;

    Map<String, String> flowParameters;
    
    Map<String, String> flowVariables;

    Map<String, String> globalSettings;

    ComponentStatistics componentStatistics = new ComponentStatistics();

    LinkedCaseInsensitiveMap<Object> context;  
    
    Map<Integer, IComponentRuntime> componentRuntimeByThread = new HashMap<>();
    
    boolean startStep = false;
    
    static public final ThreadLocal<String> projectVersionId = new ThreadLocal<>();

    public ComponentContext(AgentDeploy deployment, FlowStep flowStep, Flow manipulatedFlow, IExecutionTracker executionTracker,
            Map<String, IResourceRuntime> deployedResources, Map<String, String> flowParameters, Map<String, String> globalSettings,
            Map<String, String> flowVariables) {
        this.deployment = deployment;
        this.flowStep = flowStep;
        this.manipulatedFlow = manipulatedFlow;
        this.executionTracker = executionTracker;
        this.deployedResources = deployedResources;
        this.flowParameters = flowParameters == null ? Collections.synchronizedMap(new HashMap<>()) : Collections.synchronizedMap(new HashMap<>(flowParameters));
        this.flowVariables = flowVariables;
        this.globalSettings = globalSettings;
        this.context = new LinkedCaseInsensitiveMap<Object>();
    }

    public LinkedCaseInsensitiveMap<Object> getContext() {
        return context;
    }

    public void setContext(LinkedCaseInsensitiveMap<Object> context) {
        this.context = context;
    }
    
    public AgentDeploy getDeployment() {
        return deployment;
    }

    public FlowStep getFlowStep() {
        return flowStep;
    }

    public Flow getManipulatedFlow() {
        return manipulatedFlow;
    }

    public IExecutionTracker getExecutionTracker() {
        return executionTracker;
    }

    public Map<String, IResourceRuntime> getDeployedResources() {
        return deployedResources;
    }

    public IResourceRuntime getResourceRuntime() {
        return deployedResources.get(flowStep.getComponent().getResourceId());
    }

    public Map<String, String> getFlowParameters() {
        return flowParameters;
    }

    public void setComponentStatistics(ComponentStatistics componentStatistics) {
        this.componentStatistics = componentStatistics;
    }

    public ComponentStatistics getComponentStatistics() {
        return componentStatistics;
    }

    public Map<String, String> getGlobalSettings() {
        return globalSettings;
    }
    
    public void setStartStep(boolean startStep) {
        this.startStep = startStep;
    }
    
    public boolean isStartStep() {
        return startStep;
    }
    
    public Map<Integer, IComponentRuntime> getComponentRuntimeByThread() {
        return componentRuntimeByThread;
    }

    public Map<String, String> getFlowVariables() {
        return flowVariables;
    }

}
