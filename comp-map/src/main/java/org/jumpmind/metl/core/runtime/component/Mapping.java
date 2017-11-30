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
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Mapping extends AbstractMapping {

    public static final String TYPE = "Mapping";

    public final static String ATTRIBUTE_MAPS_TO = "mapping.processor.attribute.maps.to";

    public final static String SET_UNMAPPED_ATTRIBUTES_TO_NULL = "set.unmapped.attributes.to.null";

    public final static String ENTITY_PER_ROW = "entity.per.record";

    boolean setUnmappedAttributesToNull;
    
    Map<String, Set<String>> attrToAttrMap;

    boolean entityPerRecord;

    @Override
    public void start() {
        validate();
        setUnmappedAttributesToNull = getComponent().getBoolean(SET_UNMAPPED_ATTRIBUTES_TO_NULL,
                false);
        entityPerRecord = getComponent().getBoolean(ENTITY_PER_ROW, false);
        attrToAttrMap = getAttribToAttribMap();
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    protected void validate() {
        String message = "The ";
        if (getComponent().getInputModel() == null) {
            message = message + "input model must be configured ";
        }

        if (getComponent().getOutputModel() == null) {
            if (isNotBlank(message)) {
                message = message + " and the ";
            }
            message = message + " output model must be configured";
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
            ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();
            for (EntityData inputRow : inputRows) {
                if (entityPerRecord) {
                    outputPayload.addAll(mapInputToOutputByEntity(inputRow)); 
                } else {
                    outputPayload.addAll(mapInputToOutput(inputRow));
                }
            }
            callback.sendEntityDataMessage(null, outputPayload);
        }
    }

    protected ArrayList<EntityData> mapInputToOutputByEntity(EntityData inputRow) {
        ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();

        Model outputModel = getOutputModel();
        String entityName = null;
        HashMap<String, EntityData> outputRows = new HashMap<String, EntityData>();

        for (Entry<String, Object> attrEntry : inputRow.entrySet()) {
            Set<String> newAttrIds = attrToAttrMap.get(attrEntry.getKey());
            if (newAttrIds != null) {
                for (String newAttrId : newAttrIds) {
                    entityName = outputModel
                            .getEntityById(outputModel.getAttributeById(newAttrId).getEntityId())
                            .getName();
                    EntityData outputRow = outputRows.get(entityName);
                    if (outputRow == null) {
                        outputRow = new EntityData();
                        outputRow.setChangeType(inputRow.getChangeType());
                        outputRows.put(entityName, outputRow);
                        
                    } 
                    outputRow.put(newAttrId, attrEntry.getValue());
                }
            }
        }

        if (setUnmappedAttributesToNull) {
            for (ModelEntity entity : outputModel.getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    entityName = outputModel
                            .getEntityById(attr.getEntityId())
                            .getName();                    
                    EntityData outputRow = outputRows.get(entityName);
                    if (outputRow == null) {
                        outputRow = new EntityData();
                        outputRows.put(entityName, outputRow);   
                    }
                    if (!outputRow.containsKey(attr.getId())) {
                        outputRow.put(attr.getId(), null);
                     }                    
                }
            }
        }
        
        for (Map.Entry<String, EntityData> entry : outputRows.entrySet()) {
            EntityData outputRow = entry.getValue();
            if (outputRow.size() > 0) {
                outputPayload.add(entry.getValue());
            }
        }

        return outputPayload;
    }

    protected ArrayList<EntityData> mapInputToOutput(EntityData inputRow) {
        ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();

        EntityData outputRow = new EntityData();
        outputRow.setChangeType(inputRow.getChangeType());

        for (Entry<String, Object> attrEntry : inputRow.entrySet()) {
            Set<String> newAttrIds = attrToAttrMap.get(attrEntry.getKey());
            if (newAttrIds != null) {
                for (String newAttrId : newAttrIds) {
                    outputRow.put(newAttrId, attrEntry.getValue());
                }
            }
        }

        if (setUnmappedAttributesToNull) {
            for (ModelEntity entity : getComponent().getOutputModel().getModelEntities()) {
                for (ModelAttrib attr : entity.getModelAttributes()) {
                    if (!outputRow.containsKey(attr.getId())) {
                        outputRow.put(attr.getId(), null);
                    }
                }
            }
        }

        if (outputRow.size() > 0) {
            outputPayload.add(outputRow);
            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
        }

        return outputPayload;

    }
}
