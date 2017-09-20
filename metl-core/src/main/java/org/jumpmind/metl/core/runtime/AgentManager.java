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

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.subscribe.ISubscribeManager;
import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentManager implements IAgentManager {

    final Logger log = LoggerFactory.getLogger(getClass());

    public static final Date lastRestartTime = new Date();

    IConfigurationService configurationService;

    IExecutionService executionService;
    
    IOperationsService operationsService;

    IComponentRuntimeFactory componentRuntimeFactory;

    IDefinitionFactory definitionFactory;

    IHttpRequestMappingRegistry httpRequestMappingRegistry;
    
    ISubscribeManager subscribeManager;

    Map<String, AgentRuntime> engines = new HashMap<String, AgentRuntime>();

    public AgentManager(IOperationsService operationsService, IConfigurationService configurationService, IExecutionService executionService,
            IComponentRuntimeFactory componentFactory, IDefinitionFactory componentDefinitionFactory,
            IHttpRequestMappingRegistry httpRequestMappingRegistry, ISubscribeManager subscribeManager) {
        this.operationsService = operationsService;
        this.executionService = executionService;
        this.configurationService = configurationService;
        this.componentRuntimeFactory = componentFactory;
        this.definitionFactory = componentDefinitionFactory;
        this.httpRequestMappingRegistry = httpRequestMappingRegistry;
        this.subscribeManager = subscribeManager;
    }

    @Override
    public Set<Agent> getAvailableAgents() {
        Set<Agent> agents = new HashSet<Agent>(engines.size());
        for (AgentRuntime runtime : engines.values()) {
            agents.add(runtime.agent);
        }
        return agents;
    }

    public void start() {
        List<Agent> agents = operationsService.findAgents();
        for (Agent agent : agents) {
            createAndStartRuntime(agent);
        }
    }

    public boolean cancel(String executionId) {
        boolean cancelled = false;
        for (AgentRuntime agentRuntime : engines.values()) {
            cancelled |= agentRuntime.cancel(executionId);
        }
        return cancelled;
    }

    @Override
    public void undeploy(AgentDeploy agentDeployment) {
        AgentRuntime engine = getAgentRuntime(agentDeployment.getAgentId());
        if (engine != null) {
            engine.undeploy(agentDeployment);
        }
    }

    @Override
    public AgentDeploy deploy(String agentId, Flow flow, Map<String, String> parameters) {
        AgentDeploy deployment = null;
        AgentRuntime engine = getAgentRuntime(agentId);
        if (engine != null) {
            deployment = engine.deploy(flow, parameters);
        }
        return deployment;
    }

    @PreDestroy
    protected void destroy() {
        Collection<AgentRuntime> all = engines.values();
        for (AgentRuntime runtime : all) {
            if (runtime.isStarted()) {
                runtime.stop();
            }
        }
    }

    protected AgentRuntime createAndStartRuntime(Agent agent) {
        AgentRuntime engine = new AgentRuntime(agent, operationsService, configurationService, executionService, componentRuntimeFactory,
                definitionFactory, httpRequestMappingRegistry, subscribeManager);
        engines.put(agent.getId(), engine);
        engine.start();
        return engine;
    }

    @Override
    public AgentRuntime refresh(Agent agent) {
        AgentRuntime engine = engines.get(agent.getId());
        if (!agent.isDeleted()) {
            if (engine == null) {
                engine = createAndStartRuntime(agent);
            } else {
                engine.setAgent(agent);
                engine.stop();
                engine.start();
            }
        } else {
            if (engine != null) {
                engine.stop();
                engines.remove(agent.getId());
            }
        }
        return engine;
    }

    @Override
    public void remove(Agent agent) {
        AgentRuntime runtime = engines.get(agent.getId());
        if (runtime != null) {
            runtime.stop();
            engines.remove(agent.getId());
        }
    }

    @Override
    public AgentRuntime getAgentRuntime(String agentId) {
        return engines.get(agentId);
    }

}
