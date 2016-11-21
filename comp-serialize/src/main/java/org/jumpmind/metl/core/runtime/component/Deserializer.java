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

import static org.jumpmind.metl.core.runtime.component.ComponentSettingsConstants.STRUCTURE_BY_TABLE;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.EntityRow;
import org.jumpmind.metl.core.model.EntityTable;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

import com.fasterxml.jackson.databind.ObjectMapper;

public class Deserializer extends AbstractSerializer {

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
        try {
            if (inputMessage instanceof TextMessage) {
                TextMessage textMessage = (TextMessage) inputMessage;
                Model outputModel = getOutputModel();
                if (outputModel != null) {
                    List<String> textList = textMessage.getPayload();
                    for (String text : textList) {
                        ObjectMapper mapper = getObjectMapper();
                        if (structure.equals(STRUCTURE_BY_TABLE)) {
                            List<EntityTable> entityTables = mapper.readValue(text,
                                    mapper.getTypeFactory().constructCollectionType(List.class,
                                            EntityTable.class));
                            ArrayList<EntityData> payload = new ArrayList<>();
                            for (EntityTable entityTable : entityTables) {
                                EntityData data = entityTable.toEntityData(outputModel);
                                if (data != null) {
                                    payload.add(data);
                                }
                            }
                        } else {
                            List<EntityRow> entityRows = mapper.readValue(text,
                                    mapper.getTypeFactory().constructCollectionType(List.class,
                                            EntityRow.class));
                            ArrayList<EntityData> payload = new ArrayList<>();
                            for (EntityRow entityRow : entityRows) {
                                EntityData data = entityRow.toEntityData(outputModel);
                                if (data != null) {
                                    payload.add(data);
                                }
                            }
                            callback.sendEntityDataMessage(inputMessage.getHeader(), payload);
                        }

                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
