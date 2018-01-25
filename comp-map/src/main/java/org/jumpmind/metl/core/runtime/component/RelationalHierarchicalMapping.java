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
    
    public final static String ENTITY_TO_ORGINATING_STEP_ID = "entity.to.originating.step.id";

    public static final String QUERY_METHOD_BY_JOIN = "BY JOIN";
    
    public static final String QUERY_METHOD_BY_QUERY = "BY QUERY";
    
    boolean setUnmappedAttributesToNull;
    
    String hierarchicalQueryMethod = QUERY_METHOD_BY_JOIN;
    
    Map<String, String> attrToAttrMap;
    
    Map<String, String> entityToEntityMap;
    
    Map<String, String> entityToOrignatingIdMap;
    
    Map<String, ArrayList<EntityData>> byQueryRowData = new HashMap<String, ArrayList<EntityData>>();
    
    Map<String, Integer> currentInputRowMap = new HashMap<String, Integer>();
    
    int currentInputRow;

    @Override
    public void start() {
        validate();
        setUnmappedAttributesToNull = getComponent().getBoolean(SET_UNMAPPED_ATTRIBUTES_TO_NULL,
                false);
        hierarchicalQueryMethod = properties.get(HIERARCHICAL_QUERY_METHOD, hierarchicalQueryMethod);               
        attrToAttrMap = getTargetAttribToAttribMap();
        entityToEntityMap = getTargetEntityToEntityMap();
        if (hierarchicalQueryMethod.equalsIgnoreCase(QUERY_METHOD_BY_QUERY)) {
        		entityToOrignatingIdMap = getTargetEntityToOriginatingIdMap();
        }
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    protected void validate() {
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
            if (hierarchicalQueryMethod.equalsIgnoreCase(QUERY_METHOD_BY_JOIN)) {
            		processByJoinData(inputRows, callback);
            } else {
            		loadProcessByQueryData(inputMessage.getHeader().getOriginatingStepId(), inputRows);
            }
        } else {
            if (hierarchicalQueryMethod.equalsIgnoreCase(QUERY_METHOD_BY_QUERY) && unitOfWorkBoundaryReached) {
                processByQueryInputRows(callback);
            }
        }
    }
    
    private void processByJoinData(ArrayList<EntityData> inputRows, 
    		ISendMessageCallback callback) {
    	
    		//TODO: this won't work if input records that need to be grouped are split across
    		//      inbound messages
    		ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();
    		
    		currentInputRow=0;    		
		Model outModel = getOutputModel();
		ModelEntity rootEntity = outModel.getRootElement();

		do {
			outputPayload.add(processByJoinEntity(inputRows, rootEntity));
    			currentInputRow = currentInputRow+1;
		} while (inputRows.size() > currentInputRow);
		
        callback.sendEntityDataMessage(null, outputPayload);
    }
    
    private EntityData processByJoinEntity(ArrayList<EntityData> inputRows, ModelEntity entity) {
		EntityData entityData = new EntityData();

		for (ModelAttrib attrib:entity.getModelAttributes()) {
    			if (attrib.getDataType().equals(DataType.ARRAY)) {
    				ModelEntity childEntity = getOutputModel().getEntityById(attrib.getTypeEntityId());
    				entityData.put(attrib.getId(), processByJoinEntityArray(inputRows, entity, childEntity, currentInputRow));    				
    			} else if (attrib.getDataType().equals(DataType.REF)) {
    				ModelEntity childEntity = getOutputModel().getEntityById(attrib.getTypeEntityId());
    				entityData.put(attrib.getId(), processByJoinEntity(inputRows, childEntity));
    			} else {
    				//TODO: if this has multiple sources mapped to a single target, can we automatically create a new entity here?
    				entityData.put(attrib.getId(),mapValueFromInputToOutput(attrib.getId(), inputRows.get(currentInputRow)));
    			}
		}		
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
    
    private Map<String, String> getTargetEntityToOriginatingIdMap() {
        Map<String, String> entityToOriginatingIdMap = new HashMap<String, String>();
        List<ComponentEntitySetting> entitySettings = getComponent().getEntitySettings();
        for (ComponentEntitySetting entitySetting : entitySettings) {
            if (entitySetting.getName().equalsIgnoreCase(ENTITY_TO_ORGINATING_STEP_ID)) {
                entityToOriginatingIdMap.put(entitySetting.getEntityId(), entitySetting.getValue());
            }
        }
        return entityToOriginatingIdMap;        
    }
    
    private ArrayList<EntityData> processByJoinEntityArray(ArrayList<EntityData> inputRows, ModelEntity parentEntity, ModelEntity childEntity, int currentInputRow) {
    		ArrayList<EntityData> entityArray = new ArrayList<EntityData>();
    		
    		List<String> cntrlBreakAttributes = getCntrlBreakAttributes(parentEntity, childEntity);
    		List<Object> cntrlBreakValues = new ArrayList<Object>();

    		for (String cntrlBreakAttribute:cntrlBreakAttributes) {
    			cntrlBreakValues.add(inputRows.get(currentInputRow).get(cntrlBreakAttribute));
    		}

    		//in join scenario, the first element should always be processed
		boolean loop=true;
    		do {    			
    			entityArray.add(processByJoinEntity(inputRows, childEntity));
    			currentInputRow++;
    			int indx=0;
    			if (currentInputRow < inputRows.size()) {
	    			for (String cntrlBreakAttrib:cntrlBreakAttributes) {
	    				if (!inputRows.get(currentInputRow).get(cntrlBreakAttrib).equals(cntrlBreakValues.get(indx))) {
	    					loop=false;
	    					currentInputRow--;
	    				}
	    			}
    			} else {
    				loop=false;
    			}
    		} while (loop);
    		return entityArray;
    }
    
    private ArrayList<String> getCntrlBreakAttributes(ModelEntity parentEntity, ModelEntity childEntity) {
        ArrayList<String> cntrlBreakAttributes = new ArrayList<String>();

        // these are the src parent and child entities of the relational model
        if (entityToEntityMap != null) {
            String srcParentEntity = entityToEntityMap.get(parentEntity.getId());
            String srcChildEntity = entityToEntityMap.get(childEntity.getId());
            for (ModelRelation relation : getInputModel().getModelRelations()) {
                for (ModelRelationMapping mapping : relation.getModelRelationMappings()) {
                    if (mapping.getSourceAttribute().getEntityId().equalsIgnoreCase(srcParentEntity)
                            && mapping.getTargetAttribute().getEntityId().equalsIgnoreCase(srcChildEntity)) {
                        cntrlBreakAttributes.add(mapping.getTargetAttribute().getId());
                    }
                }
            }
            // if no foreign keys, attempt to use parent primary key mapped in
            // the child table
            if (cntrlBreakAttributes.size() == 0) {
                for (ModelAttrib attrib : getInputModel().getEntityById(srcParentEntity).getModelAttributes()) {
                    if (attrib.isPk()) {
                        String targetAttrib = findTargetAttribBasedOnSource(attrib.getId());
                        if (targetAttrib != null) {
                            cntrlBreakAttributes.add(attrToAttrMap.get(targetAttrib));
                        }
                    }
                }
            }
            return cntrlBreakAttributes;
        } else {
            throw new MisconfiguredException(
                    "Entity mapping is required in the Hierarchical/Relational mapper.  Ensure to map at least one entity of the relational model to an entity of the hierarchical model.");
        }
    }

    private String findTargetAttribBasedOnSource(String sourceAttribId) {
        for (Map.Entry<String, String> entry:attrToAttrMap.entrySet()) {
            if (entry.getValue().equalsIgnoreCase(sourceAttribId)) {
                return entry.getKey();
            }
        }
        return null;
    }
    
    protected void loadProcessByQueryData(String originatingStepId, ArrayList<EntityData> inputRows) {

    		ArrayList<EntityData> storedEntityData = byQueryRowData.get(originatingStepId);
    		if (storedEntityData == null) {
    			storedEntityData = new ArrayList<EntityData>();
    			byQueryRowData.put(originatingStepId, storedEntityData);
    			currentInputRowMap.put(originatingStepId, new Integer(0));
    		}
    		storedEntityData.addAll(inputRows);
    }
    
    protected void processByQueryInputRows(ISendMessageCallback callback) {
        
        ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();        
    	    Model outModel = getOutputModel();
		ModelEntity rootEntity = outModel.getRootElement();
        String sourceStepId = determineSourceStepForOutputEntity(rootEntity);
        ArrayList<EntityData> rootDatas = byQueryRowData.get(sourceStepId);
        
        while (rootDatas != null && rootDatas.size() > currentInputRowMap.get(sourceStepId)) {
            outputPayload.add(processByQueryEntity(null, rootEntity));
        } 
        
        callback.sendEntityDataMessage(null, outputPayload);
        currentInputRowMap.clear();
    }
    
    protected EntityData processByQueryEntity(ModelEntity parent, ModelEntity entity) {
    		
        String sourceStepId = determineSourceStepForOutputEntity(entity);
        ArrayList<EntityData> inputRows = byQueryRowData.get(sourceStepId);
        if (inputRows != null && inputRows.size() > 0) {
            EntityData inboundRow = byQueryRowData.get(sourceStepId).get(currentInputRowMap.get(sourceStepId));
    		    EntityData entityData = new EntityData();
            for (ModelAttrib attrib:entity.getModelAttributes()) {
                if (attrib.getDataType().equals(DataType.ARRAY)) {
                    ModelEntity childEntity = getOutputModel().getEntityById(attrib.getTypeEntityId());
                    entityData.put(attrib.getId(), processByQueryEntityArray(entity, childEntity));
                } else if (attrib.getDataType().equals(DataType.REF)) {
                    ModelEntity childEntity = getOutputModel().getEntityById(attrib.getTypeEntityId());
                    entityData.put(attrib.getId(), processByQueryEntity(entity, childEntity));
                } else {
                    entityData.put(attrib.getId(),mapValueFromInputToOutput(attrib.getId(), inboundRow));
                }
    		    }
            if (parent == null || !sourceStepId.equalsIgnoreCase(determineSourceStepForOutputEntity(parent))) {
                currentInputRowMap.put(sourceStepId, currentInputRowMap.get(sourceStepId).intValue()+1);
            }
        		return entityData;
        }
        return null;
    }
    
    protected ArrayList<EntityData> processByQueryEntityArray(ModelEntity parentEntity, ModelEntity childEntity) {

        String childSourceStepId = determineSourceStepForOutputEntity(childEntity);
        ArrayList<EntityData> childRows = byQueryRowData.get(childSourceStepId);
        
        if (childRows != null && childRows.size()>0) {
            ArrayList<EntityData> entityArray = new ArrayList<EntityData>();
            String parentSourceStepId = determineSourceStepForOutputEntity(parentEntity);
            ArrayList<EntityData> parentRows = byQueryRowData.get(parentSourceStepId);
            
            List<String> cntrlBreakAttributes = getCntrlBreakAttributes(parentEntity, childEntity);
            List<Object> cntrlBreakValues = new ArrayList<Object>();
    
            for (String cntrlBreakAttribute:cntrlBreakAttributes) {
                cntrlBreakValues.add(parentRows.get(currentInputRowMap.get(parentSourceStepId)).get(cntrlBreakAttribute));
            }
    
            //the first element should always be processed
            boolean loop=true;
            do {                
                entityArray.add(processByQueryEntity(parentEntity, childEntity));
                int indx=0;
                if (currentInputRowMap.get(childSourceStepId) < childRows.size() &&
                        !childSourceStepId.equalsIgnoreCase(parentSourceStepId)) {
                    for (String cntrlBreakAttrib:cntrlBreakAttributes) {
                        if (!childRows.get(currentInputRowMap.get(childSourceStepId)).get(cntrlBreakAttrib).equals(cntrlBreakValues.get(indx))) {
                            loop=false;
                            currentInputRowMap.put(childSourceStepId, currentInputRowMap.get(childSourceStepId).intValue()-1);
                        }
                    }
                } else {
                    loop=false;
                }
            } while (loop);
            return entityArray;    
        }
        return null;            
    }
    
    protected String determineSourceStepForOutputEntity(ModelEntity entity) {
    		
        String originatingStepId=entityToOrignatingIdMap.get(entity.getId());
        if (originatingStepId == null) {
            //TODO: This works for specified right now, but not implied.  Should we 
            //    allow an implied based on attribute to attribute mapping?                
        }
        return originatingStepId;
    }
}
