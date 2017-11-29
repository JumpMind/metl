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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelRelation;
import org.jumpmind.metl.core.model.ModelRelationMapping;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class RelationalHierarchicalMapping extends AbstractMapping {

    public static final String TYPE = "RelationalHierarchicalMapping";
    
    public final static String SET_UNMAPPED_ATTRIBUTES_TO_NULL = "set.unmapped.attributes.to.null";

    public final static String HIERARCHICAL_QUERY_METHOD = "hierarchical.query.method";

    public static final String QUERY_METHOD_BY_JOIN = "BY JOIN";
    
    public static final String QUERY_METHOD_BY_TABLE = "BY TABLE";
    
    boolean setUnmappedAttributesToNull;
    
    String hierarchicalQueryMethod = QUERY_METHOD_BY_JOIN;
    
    Map<String, String> attrToAttrMap;
    
    Map<String, String> entityToEntityMap;

    @Override
    public void start() {
        validate();
        setUnmappedAttributesToNull = getComponent().getBoolean(SET_UNMAPPED_ATTRIBUTES_TO_NULL,
                false);
        hierarchicalQueryMethod = properties.get(HIERARCHICAL_QUERY_METHOD, hierarchicalQueryMethod);               
        attrToAttrMap = getTargetAttribToAttribMap();
        entityToEntityMap = getTargetEntityToEntityMap();
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    protected void validate() {
    		if (hierarchicalQueryMethod.equalsIgnoreCase(QUERY_METHOD_BY_TABLE)) {
    			throw new UnsupportedOperationException("Query by Table method not yet supported");
    		}
    	
    		String message = "The ";
        if (getComponent().getInputModel() == null ||
        		!getComponent().getInputModel().getType().equalsIgnoreCase(Model.TYPE_RELATIONAL)) {
            message = message + "input model must be configured and be a relational model ";
        }

        if (getComponent().getOutputModel() == null ||
        		!getComponent().getOutputModel().getType().equalsIgnoreCase(Model.TYPE_HIERARCHICAL)) {
        	
            if (!message.equalsIgnoreCase("The ")) {
                message = message + " and the ";
            }
            message = message + " output model must be configured and be a hierarchical model";
        }

        if (!message.equals("The ")) {
            throw new MisconfiguredException(message);
        }
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
    	
        if (inputMessage instanceof EntityDataMessage) {
            ArrayList<EntityData> inputRows = ((EntityDataMessage) inputMessage).getPayload();
            if (inputRows == null) {
                return;
            }
            ArrayList<EntityData> outputPayload;
            if (hierarchicalQueryMethod.equalsIgnoreCase(QUERY_METHOD_BY_JOIN)) {
            		outputPayload = processByJoinData(inputRows);
            } else {
            		outputPayload = processByQueryData(inputRows);
            }
            callback.sendEntityDataMessage(null, outputPayload);
        }
    }
    
    private ArrayList<EntityData> processByJoinData(ArrayList<EntityData> inputRows) {
    		ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();
    		
    		int currentInputRow=0;    		
		Model outModel = getOutputModel();
		ModelEntity rootEntity = outModel.getRootElement();
		//TODO: this could also be an array of entities from a json perspective 
		//      need to address model issues with this as well unless we want
		//		to force all models to have a root...
		outputPayload.add(processEntity(inputRows, rootEntity, currentInputRow));
		
    		return outputPayload;
    }
    
    private EntityData processEntity(ArrayList<EntityData> inputRows, ModelEntity entity, int currentInputRow) {
		EntityData entityData = new EntityData();

		for (ModelAttrib attrib:entity.getModelAttributes()) {
    			if (attrib.getDataType().equals(DataType.ARRAY)) {
    				ModelEntity childEntity = getOutputModel().getEntityById(attrib.getTypeEntityId());
    				entityData.put(attrib.getId(), processEntityArray(inputRows, entity, childEntity, currentInputRow));    				
    			} else if (attrib.getDataType().equals(DataType.REF)) {
    				ModelEntity childEntity = getOutputModel().getEntityById(attrib.getTypeEntityId());
    				entityData.put(attrib.getId(), processEntity(inputRows, childEntity, currentInputRow));
    			} else {
    				entityData.put(attrib.getId(),mapValueFromInputToOutput(attrib.getId(), inputRows.get(currentInputRow)));
    			}
		}		
    		currentInputRow++;
    		return entityData;
    }

    private Object mapValueFromInputToOutput(String targetAttribId, EntityData sourceData) {
		return sourceData.get(attrToAttrMap.get(targetAttribId));
    }

    private Map<String, String> getTargetAttribToAttribMap() {
		Map<String, String>attrToAttrMap = new HashMap<String, String>();
        List<ComponentAttribSetting> attributeSettings = getComponent().getAttributeSettings();
        for (ComponentAttribSetting attributeSetting : attributeSettings) {
            if (attributeSetting.getName().equalsIgnoreCase(ATTRIBUTE_MAPS_TO)) {
            		attrToAttrMap.put(attributeSetting.getValue(), attributeSetting.getAttributeId());
            }
        }
        return attrToAttrMap;
    }
    
    private Map<String, String> getTargetEntityToEntityMap() {
		Map<String, String>entityToEntityMap = new HashMap<String, String>();
        List<ComponentEntitySetting> entitySettings = getComponent().getEntitySettings();
        for (ComponentEntitySetting entitySetting : entitySettings) {
            if (entitySetting.getName().equalsIgnoreCase(ENTITY_MAPS_TO)) {
            		entityToEntityMap.put(entitySetting.getValue(), entitySetting.getEntityId());
            }
        }
        return entityToEntityMap;
    }
    
    private ArrayList<EntityData> processEntityArray(ArrayList<EntityData> inputRows, ModelEntity parentEntity, ModelEntity childEntity, int currentInputRow) {
    		ArrayList<EntityData> entityArray = new ArrayList<EntityData>();
    		
    		List<String> cntrlBreakAttributes = getCntrlBreakAttributes(parentEntity, childEntity);
    		List<Object> cntrlBreakValues = new ArrayList<Object>();

    		for (String cntrlBreakAttribute:cntrlBreakAttributes) {
    			cntrlBreakValues.add(inputRows.get(currentInputRow).get(cntrlBreakAttribute));
    		}

    		//in join scenario, the first element should always be processed
		boolean loop=true;
    		do {    			
    			entityArray.add(processEntity(inputRows, childEntity, currentInputRow));
    			currentInputRow++;
    			int indx=0;
    			for (String cntrlBreakAttrib:cntrlBreakAttributes) {
    				if (!inputRows.get(currentInputRow).get(cntrlBreakAttrib).equals(cntrlBreakValues.get(indx))) {
    					loop=false;
    					currentInputRow--;
    				}
    			}    			
    		} while (loop);
    		return entityArray;
    }
    
    private ArrayList<String> getCntrlBreakAttributes(ModelEntity parentEntity, ModelEntity childEntity) {
    		ArrayList<String> cntrlBreakAttributes = new ArrayList<String>();
    		
    		//these are the src parent and child entities of the relational model
    		String srcParentEntity = entityToEntityMap.get(parentEntity.getId());
    		String srcChildEntity = entityToEntityMap.get(childEntity.getId());
    		for (ModelRelation relation : getInputModel().getModelRelations()) {
    			for (ModelRelationMapping mapping : relation.getModelRelationMappings()) {
    				if (mapping.getSourceAttribute().getEntityId().equalsIgnoreCase(srcParentEntity) &&
    						mapping.getTargetAttribute().getEntityId().equalsIgnoreCase(srcChildEntity)) {
    					cntrlBreakAttributes.add(mapping.getTargetAttribute().getId());
    				}
    			}
    		}
    		return cntrlBreakAttributes;
    }
    
    private ArrayList<EntityData> processByQueryData(ArrayList<EntityData> inputRows) {
		ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();
		//TODO
		return outputPayload;
    }    
}
