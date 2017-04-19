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
package org.jumpmind.metl.core.runtime.subscribe;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentProjectVersionFlowDeployment;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;

public interface ISubscribeManager {
    
    public void deploy(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep,
            XMLComponentDefinition componentDefinition);

    public void undeploy(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep,
            XMLComponentDefinition componentDefinition);
    
}
