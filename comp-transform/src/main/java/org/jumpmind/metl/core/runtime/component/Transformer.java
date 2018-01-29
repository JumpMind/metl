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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptException;

import org.apache.commons.collections.CollectionUtils;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Transformer extends AbstractComponentRuntime {

    public static final String TYPE = "Transformer";
    
    public static String TRANSFORM_EXPRESSION = "transform.expression";
    
    public static String PASS_ALONG_CONTROL_MESSAGES = "pass.along.control.messages";

    Map<String, String> transformsByAttributeId = new HashMap<String, String>();
    
    GroovyScriptEngineImpl scriptEngine;
    
    Map<String, ModelAttributeScriptHelper> helpers = new HashMap<>();
       
    long totalTime = 0;
    long totalCalls = 0;
    
    @Override
    public void start() {
        transformsByAttributeId.clear();
        
        List<ComponentAttribSetting> settings = getComponent().getAttributeSettings();
        for (ComponentAttribSetting setting : settings) {
            if (setting.getName().equals(TRANSFORM_EXPRESSION)) {
                if (isNotBlank(setting.getValue())) {
                    transformsByAttributeId.put(setting.getAttributeId(), setting.getValue());
                }
            }
        }     
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }   
    
    protected Set<String> getAllAttributesForIncludedEntities(EntityData data) {
        Set<String> allAttributesForIncludedEntities = new HashSet<>();
        Model inputModel = getComponent().getInputModel();
        Set<String> attributeIds = data.keySet();
        for (String attributeId : attributeIds) {
            ModelAttrib attribute = inputModel.getAttributeById(attributeId);
            if (attribute != null) {
                ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                List<ModelAttrib> attributes = entity.getModelAttributes();
                for (ModelAttrib modelAttribute : attributes) {
                    allAttributesForIncludedEntities.add(modelAttribute.getId());
                }
            } else {
                log.warn("Found an attribute that wasn't in the configured model.  The attribute id was: {}", attributeId);
            }
        }
        return allAttributesForIncludedEntities;
    }

    @Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (scriptEngine == null) {
            scriptEngine = new GroovyScriptEngineImpl();
        }
        totalTime = 0;
		if (inputMessage instanceof EntityDataMessage) {
			Model inputModel = getComponent().getInputModel();
			List<EntityData> inDatas = ((EntityDataMessage)inputMessage).getPayload();
			ArrayList<EntityData> outDatas = new ArrayList<EntityData>(inDatas != null ? inDatas.size() : 0);

			if (inDatas != null) {
				for (EntityData inData : inDatas) {
					outDatas.add(processEntity(inData, inputMessage, inputModel, true));
				}
			}
			callback.sendEntityDataMessage(null, outDatas);
			
			if (totalCalls > 0) {
			   log.debug("It took " + (totalTime/totalCalls) + "ms on average to call eval");
			}
						
		} else if (inputMessage instanceof ControlMessage && properties.is(PASS_ALONG_CONTROL_MESSAGES, false)) {
		        callback.sendControlMessage();
		}
	}
    
    @SuppressWarnings("unchecked")
    protected EntityData processEntity(EntityData inData, Message inputMessage, Model inputModel,
    		boolean isRoot) {

    		EntityData outData = new EntityData();
		outData.setChangeType(inData.getChangeType());
		Set<String> attributeIds = new HashSet<String>();
		attributeIds.addAll(inData.keySet());					
		attributeIds.addAll(CollectionUtils.intersection(getAllAttributesForIncludedEntities(inData), transformsByAttributeId.keySet()));		
		for (String attributeId : attributeIds) {
			processAttribute(attributeId, inData, outData, inputMessage, inputModel);
		}
		getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);

		return outData;
    }

    protected ArrayList<EntityData> processEntityArray(ArrayList<EntityData> inDatas, Message inputMessage, Model inputModel,
            boolean isRoot) {

        ArrayList<EntityData> outDatas = new ArrayList<EntityData>();
        for (EntityData inData:inDatas) {
            outDatas.add(processEntity(inData, inputMessage, inputModel, isRoot));
        }
        return outDatas;
    }    

    @SuppressWarnings("unchecked")
    protected void processAttribute(String attributeId, EntityData inData, EntityData outData, Message inputMessage, Model inputModel) {
        String transform = transformsByAttributeId.get(attributeId);
        Object value = inData.get(attributeId);
        ModelAttrib attribute = inputModel.getAttributeById(attributeId);

        if (value != null && attribute.getDataType().equals(DataType.ARRAY)) {
            outData.put(attributeId, processEntityArray((ArrayList<EntityData>) value, inputMessage, inputModel, false));
        } else if (value != null && attribute.getDataType().equals(DataType.REF)) {
            outData.put(attributeId, processEntity((EntityData) value, inputMessage, inputModel, false));
        } else {
            if (isNotBlank(transform)) {
                ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                ModelAttributeScriptHelper helper = helpers.get(attribute.getId());
                if (helper == null) {
                    long ts = System.currentTimeMillis();
                    scriptEngine.put("entity", entity);
                    scriptEngine.put("attribute", attribute);
                    scriptEngine.put("context", context);
                    scriptEngine.put("model", getInputModel());
                    try {
                        String importString = "import org.jumpmind.metl.core.runtime.component.ModelAttributeScriptHelper;\n";
                        String code = String.format(
                                "return new ModelAttributeScriptHelper(context, attribute, entity, model) { public Object eval() { %s \n } }",
                                transform);
                        helper = (ModelAttributeScriptHelper) scriptEngine.eval(importString + code);
                        helpers.put(attribute.getId(), helper);
                    } catch (ScriptException e) {
                        throw new RuntimeException("Unable to evaluate groovy script.  Attribute ==> " + attribute.getName() + ".  Value ==> "
                                + (value == null ? "null" : value.toString()) + "." + e.getCause().getMessage(), e);
                    }
                    log.debug("It took " + (System.currentTimeMillis() - ts) + "ms to create class");
                }
                helper.setData(inData);
                helper.setValue(value);
                helper.setMessage(inputMessage);
                long ts = System.currentTimeMillis();
                try {
                    value = helper.eval();
                } catch (Exception e) {
                    throw new RuntimeException("Groovy script evaluation resulted in an exception.  Attribute ==> " + attribute.getName()
                            + ".  Value ==> " + (value == null ? "null" : value.toString()) + ".  Payload ==>\n"
                            + getComponent().toRow(inData, false, false), e);
                }
                totalTime += (System.currentTimeMillis() - ts);
                totalCalls++;
            }
            if (value != ModelAttributeScriptHelper.REMOVE_ATTRIBUTE) {
                outData.put(attributeId, value);
            }
        }
    }
}
