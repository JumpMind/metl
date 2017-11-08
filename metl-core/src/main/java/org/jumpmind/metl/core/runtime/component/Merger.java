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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Merger extends AbstractComponentRuntime {

    public static final String TYPE = "Merger";

    public final static String MERGE_ATTRIBUTE = "merge.attribute";

    Map<Object, EntityData> mergedData = new LinkedHashMap<Object, EntityData>();

    List<String> attributesToMergeOn = new ArrayList<String>();

    @Override
    public void start() {
        Component component = getComponent();
        Model inputModel = component.getInputModel();
        if (inputModel == null) {
            throw new MisconfiguredException("The input model is required and has not yet been set");
        }
        
        List<ComponentAttribSetting> settings = component.getAttributeSettings();
        if (settings != null) {
            for (ComponentAttribSetting componentAttributeSetting : settings) {
                if (componentAttributeSetting.getName().equals(MERGE_ATTRIBUTE)
                        && Boolean.parseBoolean(componentAttributeSetting.getValue())) {
                    attributesToMergeOn.add(componentAttributeSetting.getAttributeId());
                }
            }
        }

        if (attributesToMergeOn.size() == 0) {
            throw new MisconfiguredException("At least one attribute must be selected for joining.");
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
            join(payload);
        }

        if (unitOfWorkBoundaryReached) {
            ArrayList<EntityData> dataToSend = new ArrayList<EntityData>();
            Iterator<EntityData> itr = mergedData.values().iterator();
            while (itr.hasNext()) {
                if (dataToSend.size() >= properties.getInt(ROWS_PER_MESSAGE)) {
                    callback.sendEntityDataMessage(null, dataToSend);
                    dataToSend = new ArrayList<EntityData>();
                }
                dataToSend.add(itr.next());
            }
            if (dataToSend != null && dataToSend.size() > 0) {
                callback.sendEntityDataMessage(null, dataToSend);
            }
        }
    }

    private void join(ArrayList<EntityData> records) {
        for (EntityData entityData : records) {
            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
            StringBuilder key = new StringBuilder();
            for (String attributeId : attributesToMergeOn) {
                if (key.length() > 0) {
                    key.append("&");
                }
                key.append(attributeId);
                key.append("=");
                key.append(entityData.get(attributeId));
            }
            Object keyValue = key.toString();
            EntityData existingRecord = mergedData.get(keyValue);
            if (existingRecord != null) {
                mergeRecords(entityData, existingRecord);
            } else {
                mergedData.put(keyValue, entityData.copy());
            }
        }
    }

    private void mergeRecords(EntityData sourceRecord, EntityData targetRecord) {
        Iterator<Map.Entry<String, Object>> itr = sourceRecord.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry<String, Object> column = (Map.Entry<String, Object>) itr.next();
            if (column != null) {
                if (column.getValue() != null) {
                    targetRecord.put(column.getKey(), column.getValue());
                }
            }
        }
    }
}
