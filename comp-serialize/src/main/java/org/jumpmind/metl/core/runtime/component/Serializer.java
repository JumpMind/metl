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

import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.FORMAT;
import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.STRUCTURE_BY_TABLE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.EntityRow;
import org.jumpmind.metl.core.model.EntityTable;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.util.FormatUtils;

public class Serializer extends AbstractSerializer {

    List<EntityData> payload;

    @Override
    public void start() {
        super.start();
        payload = new ArrayList<>();
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
        try {
            if (inputMessage instanceof EntityDataMessage) {
                EntityDataMessage entityMessage = (EntityDataMessage) inputMessage;
                payload.addAll(entityMessage.getPayload());
            }

            if (unitOfWorkBoundaryReached) {
                ArrayList<?> response = null;
                if (STRUCTURE_BY_TABLE.equals(structure)) {
                    response = createByTablePayload(payload);
                } else {
                    response = createByInboundRowPayload(payload);
                }
                Map<String,Serializable> header = new HashMap<>(inputMessage.getHeader());
                header.put(FORMAT, getDetectedFormat());
                callback.sendTextMessage(header,
                        getObjectMapper().writeValueAsString(response));
                payload = new ArrayList<>();
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ArrayList<EntityRow> createByInboundRowPayload(List<EntityData> payload) {
        ArrayList<EntityRow> entityResponse = new ArrayList<>();
        if (payload != null) {
            Model inputModel = getInputModel();
            for (EntityData entityData : payload) {
                for (ModelEntity entity : inputModel.getModelEntities()) {
                    EntityRow row = null;
                    for (ModelAttrib attribute : entity.getModelAttributes()) {
                        if (entityData.containsKey(attribute.getId())) {
                            if (row == null) {
                                row = new EntityRow(entity.getName(),
                                        new HashMap<>(entity.getModelAttributes().size()));
                                entityResponse.add(row);
                            }
                            String stringValue = null;
                            Object value = entityData.get(attribute.getId());
                            if (value instanceof Date) {
                                stringValue = FormatUtils.TIMESTAMP_FORMATTER.format((Date) value);
                            }
                            if (value != null) {
                                stringValue = value.toString();
                            }
                            row.getData().put(attribute.getName(), stringValue);
                        }
                    }
                }
            }
        }
        return entityResponse;
    }

    private ArrayList<EntityTable> createByTablePayload(List<EntityData> payload) {
        Map<String, EntityTable> entityTables = new HashMap<String, EntityTable>();
        if (payload != null) {
            Model inputModel = getInputModel();
            for (EntityData entityData : payload) {
                Iterator<String> itr = entityData.keySet().iterator();
                boolean firstAttribute = true;
                ModelEntity entity = null;
                Map<String, String> row = new HashMap<String, String>();
                while (itr.hasNext()) {
                    String attributeId = itr.next();
                    if (firstAttribute) {
                        entity = inputModel.getEntityById(
                                inputModel.getAttributeById(attributeId).getEntityId());
                        if (!entityTables.containsKey(entity.getName())) {
                            entityTables.put(entity.getName(), new EntityTable(entity.getName()));
                        }
                        firstAttribute = false;
                    }

                    String stringValue = null;
                    Object value = entityData.get(attributeId);
                    if (value instanceof Date) {
                        stringValue = FormatUtils.TIMESTAMP_FORMATTER.format((Date) value);
                    }
                    if (value != null) {
                        stringValue = value.toString();
                    }
                    row.put(inputModel.getAttributeById(attributeId).getName(), stringValue);
                }
                entityTables.get(entity.getName()).getRows().add(row);
            }
        }
        return new ArrayList<>(entityTables.values());        
    }
}
