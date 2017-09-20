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

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jumpmind.metl.core.runtime.FlowConstants.REQUEST_VALUE_PARAMETER;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import javax.jms.TextMessage;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.AgentProjectVersionFlowDeployment;
import org.jumpmind.metl.core.model.Execution;
import org.jumpmind.metl.core.model.ExecutionStatus;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.persist.IExecutionService;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.ISubscribe;
import org.jumpmind.util.AppUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubscribeManager implements ISubscribeManager {

    final Logger log = LoggerFactory.getLogger(getClass());

    Map<AgentDeploy, Subscription> mappingsByAgentDeployment = new HashMap<>();

    @Resource
    IAgentManager agentManager;
    
    @Resource
    IExecutionService executionService;

    public void deploy(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep,
            XMLComponentDefinition componentDefinition) {
        if (!agent.isDesignTimeAgent()) {
            log.info("Deploying message listener for deployment: '{}' on agent: '{}'", agentProjectVersionFlowDeployment.getName(),
                    agent.getName());
            AgentRuntime agentRuntime = agentManager.getAgentRuntime(agent.getId());
            IResourceRuntime resourceRuntime = agentRuntime.getDeployedResource(flowStep.getComponent().getResourceId());
            if (resourceRuntime instanceof ISubscribe) {
                ISubscribe subscribe = (ISubscribe) resourceRuntime;
                Subscription subscription = new Subscription(agent, agentProjectVersionFlowDeployment, flowStep, componentDefinition);
                mappingsByAgentDeployment.put(agentProjectVersionFlowDeployment.getAgentDeployment(), subscription);
                subscribe.start(subscription);
            }
        } else {
            log.info("This is a design time agent.  NOT deploying message listener for deployment: '{}' on agent: '{}'",
                    agentProjectVersionFlowDeployment.getName(), agent.getName());
        }

    }

    public void undeploy(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep,
            XMLComponentDefinition componentDefinition) {
        if (!agent.isDesignTimeAgent()) {
            log.info("Undeploying message listener for deployment: '{}' on agent: '{}'", agentProjectVersionFlowDeployment.getName(),
                    agent.getName());
            AgentRuntime agentRuntime = agentManager.getAgentRuntime(agent.getId());
            IResourceRuntime resourceRuntime = agentRuntime.getDeployedResource(flowStep.getComponent().getResourceId());
            if (resourceRuntime instanceof ISubscribe) {
                ISubscribe subscribe = (ISubscribe) resourceRuntime;
                Subscription subscription = mappingsByAgentDeployment.remove(agentProjectVersionFlowDeployment.getAgentDeployment());
                if (subscription != null) {
                    subscribe.stop(subscription);
                } else {
                    log.warn("Could not find a subscription to stop for  deployment: '{}' on agent: '{}'");
                }
            }

        } else {
            log.info("This is a design time agent.  NOT undeploying message listener for deployment: '{}' on agent: '{}'",
                    agentProjectVersionFlowDeployment.getName(), agent.getName());

        }
    }
    
    public static String getPayload(Message message, String mapTypeKeyName) throws JMSException {
        StringBuilder builder = new StringBuilder();
        if (message instanceof TextMessage) {
            TextMessage textMessage = (TextMessage) message;
            String text = textMessage.getText();
            if (isNotBlank(text)) {
                builder.append(text);
            }
        } else if (message instanceof MapMessage) {
            MapMessage mapMessage = (MapMessage) message;
            String text = mapMessage.getString(mapTypeKeyName);
            if (isNotBlank(text)) {
                builder.append(text);
            }
        } else if (message instanceof ObjectMessage) {
            ObjectMessage objMessage = (ObjectMessage) message;
            Object obj = objMessage.getObject();
            if (obj != null) {
                builder.append(obj.toString());
            }
        } else if (message instanceof BytesMessage) {
            BytesMessage bytesMessage = (BytesMessage) message;
            long length = bytesMessage.getBodyLength();
            byte[] bytes = new byte[(int) length];
            bytesMessage.readBytes(bytes, (int) length);
        }
        return builder.toString();
    }

    class Subscription implements MessageListener {

        Agent agent;

        AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment;

        FlowStep flowStep;

        XMLComponentDefinition componentDefinition;

        public Subscription(Agent agent, AgentProjectVersionFlowDeployment agentProjectVersionFlowDeployment, FlowStep flowStep,
                XMLComponentDefinition componentDefinition) {
            this.agent = agent;
            this.agentProjectVersionFlowDeployment = agentProjectVersionFlowDeployment;
            this.flowStep = flowStep;
            this.componentDefinition = componentDefinition;
        }

        @Override
        public void onMessage(Message message) {
            try {
                log.info("Received a message: " + ((TextMessage) message).getText());
                AgentRuntime agentRuntime = agentManager.getAgentRuntime(agent.getId());
                AgentDeploy agentDeployment = agentProjectVersionFlowDeployment.getAgentDeployment();
                Map<String, String> params = new HashMap<>();
                params.put(REQUEST_VALUE_PARAMETER, getPayload(message, null));
                String executionId = agentRuntime.scheduleNow(agentDeployment.getId(), agentDeployment, params);
                Execution execution = null;
                boolean done = false;
                do {
                    execution = executionService.findExecution(executionId);
                    done = execution != null && ExecutionStatus.isDone(execution.getExecutionStatus());
                    if (!done) {
                        AppUtils.sleep(50);
                    }
                } while (!done);
            } catch (JMSException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
