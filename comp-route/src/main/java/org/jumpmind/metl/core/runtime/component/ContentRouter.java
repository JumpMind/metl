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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ContentRouter extends AbstractComponentRuntime {

    public static final String TYPE = "Content Router";

    public static final String SETTING_CONFIG = "config";

    public final static String ONLY_ROUTE_FIRST_MATCH = "only.route.first.match";

    List<Route> routes;

    ScriptEngine scriptEngine;

    boolean onlyRouteFirstMatch;

    long rowsPerMessage = 1000;
    
    Set<String> targetStepsThatNeedControlMessages = new HashSet<>();

    @Override
    public void start() {
        scriptEngine = new GroovyScriptEngineImpl();
        TypedProperties properties = getTypedProperties();
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        String json = getComponent().get(SETTING_CONFIG);
        onlyRouteFirstMatch = getComponent().getBoolean(ONLY_ROUTE_FIRST_MATCH, false);
        if (isNotBlank(json)) {
            try {
                routes = new ObjectMapper().readValue(json, new TypeReference<List<Route>>() {
                });
                // Verify all routes are valid
                for (Route route : routes) {
                    FlowStepLink link = getFlow()
                            .findLinkBetweenSourceAndTarget(this.getFlowStepId(),route.getTargetStepId());
                    if (link == null) {
                        throw new MisconfiguredException("A route target step is not linked."); 
                    }
                }
            } catch (Exception e) {
                throw new IoException(e);
            }
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
            handleEntityListPayload((EntityDataMessage)inputMessage, callback, unitOfWorkBoundaryReached);
        } else if (inputMessage instanceof TextMessage) {
            handleStringListPayload((TextMessage)inputMessage, callback, unitOfWorkBoundaryReached);
        } else if (inputMessage instanceof ControlMessage) {
            handleControlMessages((ControlMessage)inputMessage, callback, unitOfWorkBoundaryReached);
        }
        
        if (unitOfWorkBoundaryReached) {
            for (String targetStepId : targetStepsThatNeedControlMessages) {
                callback.sendControlMessage(null, targetStepId);
            }
        }
    }

    void handleEntityListPayload(EntityDataMessage inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        Map<String, ArrayList<EntityData>> outboundMessages = new HashMap<String, ArrayList<EntityData>>();
        ArrayList<EntityData> inputDatas = inputMessage.getPayload();

        for (EntityData entityData : inputDatas) {
            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
            bindEntityData(scriptEngine, inputMessage, entityData);
            if (routes != null) {
                for (Route route : routes) {
                    try {
                        if (Boolean.TRUE.equals(scriptEngine.eval(route.getMatchExpression()))) {
                            ArrayList<EntityData> outboundPayload = outboundMessages.get(route.getTargetStepId());
                            if (outboundPayload != null && outboundPayload.size() >= rowsPerMessage) {
                                outboundMessages.remove(route.getTargetStepId());
                                callback.sendEntityDataMessage(null, outboundPayload, route.getTargetStepId());
                                targetStepsThatNeedControlMessages.add(route.getTargetStepId());
                                outboundPayload = null;
                            }
                            if (outboundPayload == null) {
                                outboundPayload = new ArrayList<EntityData>();
                                outboundMessages.put(route.getTargetStepId(), outboundPayload);
                            }
                            outboundPayload.add(entityData.copy());
                            if (onlyRouteFirstMatch) {
                                break;
                            }
                        }
                    } catch (ScriptException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        for (String targetFlowStepId : outboundMessages.keySet()) {
            callback.sendEntityDataMessage(null, outboundMessages.get(targetFlowStepId), targetFlowStepId);
            targetStepsThatNeedControlMessages.add(targetFlowStepId);
        }
    }

    protected void handleControlMessages(ControlMessage inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        Bindings bindings = scriptEngine.createBindings();
        bindHeadersAndFlowParameters(bindings, inputMessage);
        scriptEngine.setBindings(bindings, ScriptContext.ENGINE_SCOPE);
        if (routes != null) {
            for (Route route : routes) {
                try {
                    if (Boolean.TRUE.equals(scriptEngine.eval(route.getMatchExpression()))) {
                        callback.sendControlMessage(inputMessage.getHeader(), route.getTargetStepId());
                        targetStepsThatNeedControlMessages.remove(route.getTargetStepId());
                        if (onlyRouteFirstMatch) {
                            break;
                        }
                    }
                } catch (ScriptException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    protected void handleStringListPayload(TextMessage inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        Map<String, ArrayList<String>> outboundMessages = new HashMap<String, ArrayList<String>>();
        ArrayList<String> inputDatas = (ArrayList<String>) inputMessage.getPayload();
        for (String data : inputDatas) {
            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
            bindStringData(scriptEngine, inputMessage, data);
            if (routes != null) {
                for (Route route : routes) {
                    try {
                        if (Boolean.TRUE.equals(scriptEngine.eval(route.getMatchExpression()))) {
                            ArrayList<String> outboundPayload = outboundMessages.get(route.getTargetStepId());
                            if (outboundPayload == null) {
                                outboundPayload = new ArrayList<String>();
                                outboundMessages.put(route.getTargetStepId(), outboundPayload);
                            }
                            if (outboundPayload.size() >= rowsPerMessage) {
                                outboundMessages.remove(route.getTargetStepId());
                                callback.sendTextMessage(null, outboundPayload, route.getTargetStepId());
                                targetStepsThatNeedControlMessages.add(route.getTargetStepId());
                            }
                            outboundPayload.add(data);
                            if (onlyRouteFirstMatch) {
                                break;
                            }
                        }
                    } catch (ScriptException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        for (String targetFlowStepId : outboundMessages.keySet()) {
            callback.sendTextMessage(null, outboundMessages.get(targetFlowStepId), targetFlowStepId);
            targetStepsThatNeedControlMessages.add(targetFlowStepId);
        }

    }

    static public class Route implements Serializable {
        
        private static final long serialVersionUID = 1L;
        
        String matchExpression;
        String targetStepId;

        public Route() {
        }

        public Route(String matchExpression, String targetStepId) {
            this.matchExpression = matchExpression;
            this.targetStepId = targetStepId;
        }

        public String getMatchExpression() {
            return matchExpression;
        }

        public void setMatchExpression(String matchExpression) {
            this.matchExpression = matchExpression;
        }

        public String getTargetStepId() {
            return targetStepId;
        }

        public void setTargetStepId(String targetStepId) {
            this.targetStepId = targetStepId;
        }
    }

}
