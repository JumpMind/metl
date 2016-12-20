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
import java.util.Map;
import java.util.Set;

import javax.annotation.PreDestroy;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.AgentStartMode;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.jumpmind.metl.core.runtime.web.IHttpRequestMappingRegistry;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AgentManager implements IAgentManager {

    final Logger log = LoggerFactory.getLogger(getClass());

    public static final Date lastRestartTime = new Date();

    IConfigurationService configurationService;

    IExecutionService executionService;

    IComponentRuntimeFactory componentRuntimeFactory;

    IDefinitionFactory definitionFactory;

    IHttpRequestMappingRegistry httpRequestMappingRegistry;

    Map<String, AgentRuntime> engines = new HashMap<String, AgentRuntime>();

    public AgentManager(IConfigurationService configurationService, IExecutionService executionService,
            IComponentRuntimeFactory componentFactory, IDefinitionFactory componentDefinitionFactory,
            IHttpRequestMappingRegistry httpRequestMappingRegistry) {
        this.executionService = executionService;
        this.configurationService = configurationService;
        this.componentRuntimeFactory = componentFactory;
        this.definitionFactory = componentDefinitionFactory;
        this.httpRequestMappingRegistry = httpRequestMappingRegistry;
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
        Set<Agent> agents = findLocalAgents();
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
    public void undeploy(AgentDeployment agentDeployment) {
        AgentRuntime engine = getAgentRuntime(agentDeployment.getAgentId());
        if (engine != null) {
            engine.undeploy(agentDeployment);
        }
    }

    @Override
    public AgentDeployment deploy(String agentId, Flow flow, Map<String, String> parameters) {
        AgentDeployment deployment = null;
        AgentRuntime engine = getAgentRuntime(agentId);
        if (engine != null) {
            deployment = engine.deploy(flow, parameters);
        }
        return deployment;
    }

    protected Set<Agent> findLocalAgents() {
        Set<Agent> agents = new HashSet<Agent>(configurationService.findAgentsForHost(AppUtils.getHostName()));
        agents.addAll(configurationService.findAgentsForHost(AppUtils.getIpAddress()));
        agents.addAll(configurationService.findAgentsForHost("localhost"));
        return agents;
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

    @Override
    public boolean isAgentLocal(Agent agent) {
        String hostName = agent.getHost();
        return "localhost".equals(hostName) || "127.0.0.1".equals(hostName) || "::1".equals(hostName)
                || AppUtils.getHostName().equals(hostName) || AppUtils.getIpAddress().equals(hostName);
    }

    protected AgentRuntime createAndStartRuntime(Agent agent) {
        AgentRuntime engine = new AgentRuntime(agent, configurationService, executionService, componentRuntimeFactory, 
                definitionFactory, httpRequestMappingRegistry);
        engines.put(agent.getId(), engine);
        if (agent.getAgentStartMode() == AgentStartMode.AUTO) {
            engine.start();
        } else {
            log.info("The '{}' agent is configured to be started manually. It will not be auto started.", agent.toString());
        }
        return engine;
    }

    @Override
    public AgentRuntime refresh(Agent agent) {
        AgentRuntime engine = engines.get(agent);
        if (isAgentLocal(agent) && !agent.isDeleted()) {
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
                engines.remove(agent);
            }
        }
        return engine;
    }

    @Override
    public void remove(Agent agent) {
        AgentRuntime runtime = engines.get(agent);
        if (runtime != null) {
            runtime.stop();
            engines.remove(agent);
        }
    }

    @Override
    public AgentRuntime getAgentRuntime(String agentId) {
        return engines.get(agentId);
    }

}
