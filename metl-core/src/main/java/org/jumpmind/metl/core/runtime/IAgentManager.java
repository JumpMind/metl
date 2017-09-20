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
package org.jumpmind.metl.core.runtime;

import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.Flow;

public interface IAgentManager {

    public AgentRuntime refresh(Agent agent);
    
    public boolean cancel(String executionId);
    
    public void remove(Agent agent);
    
    public AgentRuntime getAgentRuntime(String agentId);
    
    public void undeploy(AgentDeploy deployment);
    
    public AgentDeploy deploy(String agentId, Flow flow, Map<String, String> parameters);
    
    public void start();
    
    public Set<Agent> getAvailableAgents();
    
    
}
