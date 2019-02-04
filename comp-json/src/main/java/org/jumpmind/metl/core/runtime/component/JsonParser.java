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

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.ui.views.design.EditJsonPanel;
import org.jumpmind.properties.TypedProperties;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class JsonParser extends AbstractXMLComponentRuntime {

    public static final String TYPE = "JSON Parser";

    List<JsonEntitySetting> entitySettings = new ArrayList<JsonEntitySetting>();
    
    int rowsPerMessage;

    @Override
    public void start() {
        super.start();
        TypedProperties properties = getTypedProperties();
        rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);

        RelationalModel model = (RelationalModel) getComponent().getOutputModel();
        if (model == null) {
            throw new IllegalStateException("The output model must be defined");
        }
        
        Component component = getComponent();

        for (ComponentEntitySetting compEntitySetting : component.getEntitySettings()) {
            if (compEntitySetting.getName().equals(EditJsonPanel.JSON_PATH)) {
                String entityPath = compEntitySetting.getValue();
                
                JsonEntitySetting entitySetting = new JsonEntitySetting(compEntitySetting, entityPath);
                entitySettings.add(entitySetting);
                
                List<ComponentAttribSetting> attributeSettings = component
                        .getAttributeSettingsFor(entitySetting.getSetting().getEntityId());
                for (ComponentAttribSetting componentAttributeSetting : attributeSettings) {
                    if (componentAttributeSetting.getName().equals(EditJsonPanel.JSON_PATH)) {
                        String attributePath = componentAttributeSetting.getValue();
                        entitySetting.getAttributeSettings().add(new JsonAttributeSetting(componentAttributeSetting, attributePath));
                    }
                }
            }
        }

        if (entitySettings.size() == 0) {
            throw new MisconfiguredException("At least one path setting must be provided.");
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            handleUsingXPath(inputMessage, callback, unitOfWorkBoundaryReached);
        }
    }
   
    protected void handleUsingXPath(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        ArrayList<String> inputRows = ((TextMessage) inputMessage).getPayload();
        ArrayList<EntityData> payload = new ArrayList<EntityData>();
        if (inputRows != null) {
            for (String json : inputRows) {
                try {
                	ObjectMapper mapper = new ObjectMapper();
                	JsonNode document = mapper.readTree(json);
                	
                    for (JsonEntitySetting entitySetting : entitySettings) {
                        List<JsonAttributeSetting> attributeSettings = entitySetting.getAttributeSettings();
                        
                        JsonNode entityArray = document.get(entitySetting.getPath()); 
                        if (!entityArray.isArray()) {
                        	log(LogLevel.WARN,
                                    "Found entity: '%s' is not a JSON array.  Please make sure your entity path maps to a JSON array.",
                                    entityArray.asText());
                        	continue;
                        }
                        
                        for (JsonNode entityNode : entityArray) {
                            
                            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                            EntityData data = new EntityData();
                            for (JsonAttributeSetting attributeSetting : attributeSettings) {
                            	JsonNode attributeNode = entityNode.findValue(attributeSetting.getPath());
                            	data.put(attributeSetting.getSetting().getAttributeId(), attributeNode.asText());
                                
                            	if (attributeNode != null) {
                            		switch (attributeNode.getNodeType()) {
										case BOOLEAN:
											data.put(attributeSetting.getSetting().getAttributeId(), attributeNode.asBoolean());
											break;
										case NUMBER:
											data.put(attributeSetting.getSetting().getAttributeId(), attributeNode.asDouble());
											break;
										case STRING:
											data.put(attributeSetting.getSetting().getAttributeId(), attributeNode.asText());
											break;
										default:
											break;
									}
                            	}
                            }
                            if (data.size() > 0) {
                                payload.add(data);
                            } else {
                                log(LogLevel.WARN,
                                        "Found entity element: %s with no matching attributes.  Please make sure your path expressions match",
                                        entityNode.asText());
                            }
                        }
                    }

                    if (payload.size() > rowsPerMessage) {
                        callback.sendEntityDataMessage(null, payload);
                        payload = new ArrayList<>();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (payload.size() > 0) {
            callback.sendEntityDataMessage(null, payload);
        }
    }

    class JsonAttributeSetting {

        ComponentAttribSetting setting;

        String path;

        JsonAttributeSetting(ComponentAttribSetting setting, String path) {
            this.setting = setting;
            this.path = path;
        }

        public ComponentAttribSetting getSetting() {
            return setting;
        }

        public String getPath() {
            return path;
        }
    }

    class JsonEntitySetting {

        ComponentEntitySetting setting;

        String path;

        List<JsonAttributeSetting> attributeSettings;

        JsonEntitySetting(ComponentEntitySetting setting, String path) {
            this.setting = setting;
            this.path = path;
            this.attributeSettings = new ArrayList<JsonAttributeSetting>();
        }

        public ComponentEntitySetting getSetting() {
            return setting;
        }

        public String getPath() {
            return path;
        }

        public List<JsonAttributeSetting> getAttributeSettings() {
            return attributeSettings;
        }

    }

}
