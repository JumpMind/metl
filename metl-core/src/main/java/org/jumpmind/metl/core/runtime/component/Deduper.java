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

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Deduper extends AbstractComponentRuntime {

    public static final String TYPE = "Deduper";

    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    int rowsPerMessage = 10000;

    LinkedHashMap<String, EntityData> deduped = new LinkedHashMap<String, EntityData>();

    @Override
    protected void start() {
        rowsPerMessage = getComponent().getInt(ROWS_PER_MESSAGE, rowsPerMessage);
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (!(inputMessage instanceof StartupMessage)) {
            ArrayList<EntityData> payload = inputMessage.getPayload();
            for (EntityData entityData : payload) {
                String key = entityData.toString();
                if (!deduped.containsKey(key)) {
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                    deduped.put(key, entityData);
                }
            }
        }

        if (unitOfWorkBoundaryReached) {
            if (deduped.size() > 0) {
                int count = 0;
                ArrayList<EntityData> payload = new ArrayList<EntityData>(rowsPerMessage);
                for (EntityData data : deduped.values()) {
                    if (count >= rowsPerMessage) {
                        callback.sendMessage(payload, false);
                        payload = new ArrayList<EntityData>();
                        count = 0;
                    }
                    payload.add(data);
                    count++;
                }

                deduped.clear();

                callback.sendMessage(payload, true);
            }
        }
    }

}
