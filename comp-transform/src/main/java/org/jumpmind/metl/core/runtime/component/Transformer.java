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

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.collections.CollectionUtils;
import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
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
    
    ScriptEngine scriptEngine;
    
    Map<String, ModelAttributeScriptHelper> helpers = new HashMap<>();
       
    long totalTime = 0;
    long totalCalls = 0;
    
    @Override
    public void start() {
        transformsByAttributeId.clear();
        
        List<ComponentAttributeSetting> settings = getComponent().getAttributeSettings();
        for (ComponentAttributeSetting setting : settings) {
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
            ModelAttribute attribute = inputModel.getAttributeById(attributeId);
            ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
            List<ModelAttribute> attributes = entity.getModelAttributes();
            for (ModelAttribute modelAttribute : attributes) {
                allAttributesForIncludedEntities.add(modelAttribute.getId());
            }
        }
        return allAttributesForIncludedEntities;
    }

    @SuppressWarnings("unchecked")
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
					EntityData outData = new EntityData();
					outData.setChangeType(inData.getChangeType());
					outDatas.add(outData);

					Set<String> attributeIds = new HashSet<String>();
					attributeIds.addAll(inData.keySet());					
					attributeIds.addAll(CollectionUtils.intersection(getAllAttributesForIncludedEntities(inData), transformsByAttributeId.keySet()));
					
					for (String attributeId : attributeIds) {
						String transform = transformsByAttributeId.get(attributeId);
						Object value = inData.get(attributeId);
						if (isNotBlank(transform)) {
							ModelAttribute attribute = inputModel.getAttributeById(attributeId);
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
						                    "return new ModelAttributeScriptHelper(context, attribute, entity, model) { public Object eval() { return %s } }",
						                    transform);
						            helper = (ModelAttributeScriptHelper)scriptEngine.eval(importString + code);
						            helpers.put(attribute.getId(), helper);
						        } catch (ScriptException e) {
						            throw new RuntimeException("Unable to evaluate groovy script.  Attribute ==> " + attribute.getName() + ".  Value ==> "
						                    + value.toString() + "." + e.getCause().getMessage(), e);
						        }
						        
						        log.debug("It took " + (System.currentTimeMillis()-ts) + "ms to create class");
							}
							
							helper.setData(inData);
							helper.setValue(value);
							helper.setMessage(inputMessage);
							long ts = System.currentTimeMillis();
							value = helper.eval();
							totalTime += (System.currentTimeMillis()-ts);
							totalCalls ++;
						}
						if (value != ModelAttributeScriptHelper.REMOVE_ATTRIBUTE) {
							outData.put(attributeId, value);
						}
					}
					getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
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
}
