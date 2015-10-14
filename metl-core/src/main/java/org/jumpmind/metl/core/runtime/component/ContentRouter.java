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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.util.ComponentUtil;
import org.jumpmind.properties.TypedProperties;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ContentRouter extends AbstractComponentRuntime {

    public static final String TYPE = "Content Router";

    public static final String SETTING_CONFIG = "config";

    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    public final static String ONLY_ROUTE_FIRST_MATCH = "only.route.first.match";

    List<Route> routes;

    ScriptEngine scriptEngine;

    boolean onlyRouteFirstMatch;
    
    long rowsPerMessage = 10000;

    @Override
    protected void start() {
        ScriptEngineManager factory = new ScriptEngineManager();
        scriptEngine = factory.getEngineByName("groovy");
        TypedProperties properties = getTypedProperties();
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        String json = getComponent().get(SETTING_CONFIG);
        onlyRouteFirstMatch = getComponent().getBoolean(ONLY_ROUTE_FIRST_MATCH, false);
        if (isNotBlank(json)) {
            try {
                routes = new ObjectMapper().readValue(json, new TypeReference<List<Route>>() {
                });
            } catch (Exception e) {
                throw new IoException(e);
            }
        }
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	if (ComponentUtil.getPayloadType(inputMessage.getPayload()) == ComponentUtil.PAYLOAD_TYPE_LIST_ENTITY) {
    		handleEntityListPayload(inputMessage, callback, unitOfWorkBoundaryReached);
    	}
    	else if (ComponentUtil.getPayloadType(inputMessage.getPayload()) == ComponentUtil.PAYLOAD_TYPE_LIST_STRING) {
    		handleStringListPayload(inputMessage, callback, unitOfWorkBoundaryReached);
    	}
    }

    void handleEntityListPayload(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	Map<String, ArrayList<EntityData>> outboundMessages = new HashMap<String, ArrayList<EntityData>>();
        ArrayList<EntityData> inputDatas = inputMessage.getPayload();
        
        for (EntityData entityData : inputDatas) {
            bindEntityData(scriptEngine, entityData);
            if (routes != null) {
                for (Route route : routes) {
                	try {
                    	if (Boolean.TRUE.equals(scriptEngine.eval(route.getMatchExpression()))) {
                        	getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                        	
                        	ArrayList<EntityData> outboundPayload = outboundMessages.get(route.getTargetStepId());
                            if (outboundPayload == null) {
                                outboundPayload = new ArrayList<EntityData>();
                                outboundMessages.put(route.getTargetStepId(), outboundPayload);
                            }
                            if (outboundPayload.size() >= rowsPerMessage) {
                                outboundMessages.remove(route.getTargetStepId());
                                callback.sendMessage(outboundPayload, false, route.getTargetStepId());
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
            callback.sendMessage(outboundMessages.get(targetFlowStepId), true, targetFlowStepId);
        }

    }
    
    @SuppressWarnings("unchecked")
    void handleStringListPayload(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	Map<String, ArrayList<String>> outboundMessages = new HashMap<String, ArrayList<String>>();
        ArrayList<String> inputDatas = (ArrayList<String>) inputMessage.getPayload();
        for (String data : inputDatas) {
            bindStringData(scriptEngine, data);
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
                                callback.sendMessage(outboundPayload, false, route.getTargetStepId());
                            }
                            outboundPayload.add(data);
                        }
                    } catch (ScriptException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        for (String targetFlowStepId : outboundMessages.keySet()) {
            callback.sendMessage(outboundMessages.get(targetFlowStepId), true, targetFlowStepId);
        }

    }
    
    static public class Route {
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
