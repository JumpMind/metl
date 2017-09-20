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
import java.util.LinkedHashMap;
import java.util.List;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class Deduper extends AbstractComponentRuntime {

    public static final String TYPE = "Deduper";
    
    public static final String DEDUPE_ENTITY = "ENTITY";
    public static final String DEDUPE_ATTRIBUTE = "ATTRIBUTE";

    public static final String PRESERVE_FIRST = "First Record";
    public static final String PRESERVE_LAST = "Last Record";

    public final static String DEDUPE_TYPE = "dedupe.type";
    
    public final static String PRESERVE_RECORD = "preserve.record";

    public final static String ATTRIBUTE_DEDUPE_ENABLED = "dedupe.enabled";
    
    int rowsPerMessage = 1000;
    
    String dedupeType = DEDUPE_ENTITY;

    String dedupeKeyAttribute;
    
    ArrayList<String> dedupeKeyAttributeIdList = new ArrayList<>();
    
    String preserveRecord = PRESERVE_FIRST;

    LinkedHashMap<String, EntityData> deduped = new LinkedHashMap<String, EntityData>();

    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        rowsPerMessage = getComponent().getInt(ROWS_PER_MESSAGE, rowsPerMessage);
        dedupeType = properties.get(DEDUPE_TYPE);
        preserveRecord = properties.get(PRESERVE_RECORD);
        Model inputModel = this.getComponent().getInputModel();
        if (inputModel == null) {
            throw new MisconfiguredException("The input model is not set and it is required");
        }
        Component component = context.getFlowStep().getComponent();
        
        if (DEDUPE_ATTRIBUTE.equals(dedupeType)) {
        	List<ModelEntity> entities = new ArrayList<>(inputModel.getModelEntities());
            for (ModelEntity entity : entities) {
                for (ModelAttrib attribute : entity.getModelAttributes()) {
                	ComponentAttribSetting matchColumnSetting = component.getSingleAttributeSetting(attribute.getId(),
                            Deduper.ATTRIBUTE_DEDUPE_ENABLED);
                    boolean matchColumn = matchColumnSetting != null
                            ? Boolean.parseBoolean(matchColumnSetting.getValue()) : false;
                    if (matchColumn) {
                		// fill the list of attribute ids to dedupe on
                		dedupeKeyAttributeIdList.add(attribute.getId());
                	}
		        }
	        }
	        
	        if (dedupeKeyAttributeIdList.size() == 0) {
	        	throw new IllegalStateException(
	    				"At least one attribute must be specified when Dedupe Type of 'ATTRIBUTE' is selected.");
	        }	
        }
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
            ArrayList<EntityData> payload = ((EntityDataMessage)inputMessage).getPayload();
            for (EntityData entityData : payload) {
                String key = "";

                if (DEDUPE_ATTRIBUTE.equals(dedupeType)) {
                	for (String attributeId : dedupeKeyAttributeIdList) {
                		key += entityData.get(attributeId);
                	}
                } else {
                	key = entityData.toString();
                }
                
                if (!deduped.containsKey(key)) {
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                    deduped.put(key, entityData);
                } else {
                    // else it exists, check if we need to save the first or last value and replace if necessary
                	if (PRESERVE_LAST.equals(preserveRecord)) {
                		deduped.put(key, entityData);
                	}
                }
            }
        }

        if (unitOfWorkBoundaryReached) {
            if (deduped.size() > 0) {
                int count = 0;
                ArrayList<EntityData> payload = new ArrayList<EntityData>(rowsPerMessage);
                for (EntityData data : deduped.values()) {
                    if (count >= rowsPerMessage) {
                        callback.sendEntityDataMessage(null, payload);
                        payload = new ArrayList<EntityData>();
                        count = 0;
                    }
                    payload.add(data);
                    count++;
                }

                deduped.clear();

                callback.sendEntityDataMessage(null, payload);
            }
        }
    }

}
