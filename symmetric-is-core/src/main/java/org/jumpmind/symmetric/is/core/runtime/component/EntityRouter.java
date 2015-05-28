package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@ComponentDefinition(
        category = ComponentCategory.PROCESSOR,
        typeName = EntityRouter.TYPE,
        iconImage = "entityrouter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.ENTITY,
        inputOutputModelsMatch = true)
public class EntityRouter extends AbstractComponentRuntime {

    public static final String TYPE = "Entity Router";

    public static final String SETTING_CONFIG = "config";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.INTEGER,
            defaultValue = "10000",
            label = "Rows/Msg")
    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    List<Route> routes;

    ScriptEngine scriptEngine;

    long rowsPerMessage = 10000;

    protected void applySettings() {
        TypedProperties properties = getComponent().toTypedProperties(getSettingDefinitions(false));
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        String json = getComponent().get(SETTING_CONFIG);
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
    protected void start() {
        ScriptEngineManager factory = new ScriptEngineManager();
        scriptEngine = factory.getEngineByName("groovy");
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        Map<String, Message> outboundMessages = new HashMap<String, Message>();
        ArrayList<EntityData> inputDatas = inputMessage.getPayload();
        for (EntityData entityData : inputDatas) {
            bindEntityData(scriptEngine, entityData);
            if (routes != null) {
                for (Route route : routes) {
                    try {
                        if (Boolean.TRUE.equals(scriptEngine.eval(route.getMatchExpression()))) {
                            Message message = outboundMessages.get(route.getTargetStepId());
                            if (message == null) {
                                message = new Message(getFlowStepId());
                                message.setPayload(new ArrayList<EntityData>());
                                message.getHeader().getTargetStepIds().add(route.getTargetStepId());
                                outboundMessages.put(route.getTargetStepId(), message);
                            }
                            ArrayList<EntityData> outputRows = message.getPayload();
                            if (outputRows.size() >= rowsPerMessage) {
                                outboundMessages.remove(route.getTargetStepId());
                                getComponentStatistics().incrementOutboundMessages();
                                messageTarget.put(message);
                            }
                            outputRows.add(entityData.copy());
                        }
                    } catch (ScriptException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        }

        Collection<Message> messages = outboundMessages.values();
        for (Message message : messages) {
            getComponentStatistics().incrementOutboundMessages();
            messageTarget.put(message);
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
