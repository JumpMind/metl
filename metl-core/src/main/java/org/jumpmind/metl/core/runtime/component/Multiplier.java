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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Multiplier extends AbstractComponentRuntime {

    public static final String TYPE = "Multiplier";

    public final static String MULTIPLIER_SOURCE_STEP = "multiplier.source.step";

    boolean multipliersInitialized = false;

    String sourceStepId;

    int rowsPerMessage;

    List<EntityData> multipliers = new ArrayList<EntityData>();

    List<Message> queuedWhileWaitingForMultiplier = new ArrayList<Message>();

    @Override
    public void start() {
        multipliersInitialized = false;

        sourceStepId = getComponent().get(MULTIPLIER_SOURCE_STEP);
        rowsPerMessage = getComponent().getInt(ROWS_PER_MESSAGE, 10);

        if (isBlank(sourceStepId) || getFlow().findFlowStepWithId(sourceStepId) == null) {
            throw new IllegalStateException("The source step must be specified");
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (sourceStepId.equals(inputMessage.getHeader().getOriginatingStepId())) {
            if (inputMessage instanceof EntityDataMessage) {
                List<EntityData> datas = ((EntityDataMessage) inputMessage).getPayload();
                multipliers.addAll(datas);
            }

            multipliersInitialized = inputMessage instanceof ControlMessage;

            if (multipliersInitialized) {
                Iterator<Message> messages = queuedWhileWaitingForMultiplier.iterator();
                while (messages.hasNext()) {
                    Message message = messages.next();
                    if (message instanceof EntityDataMessage) {
                        multiply((EntityDataMessage) message, callback);
                    }
                }
            }
        } else if (!multipliersInitialized) {
            queuedWhileWaitingForMultiplier.add(inputMessage);
        } else if (multipliersInitialized) {
            if (inputMessage instanceof EntityDataMessage) {
                multiply((EntityDataMessage) inputMessage, callback);
            }
        }
    }

    protected void multiply(EntityDataMessage message, ISendMessageCallback callback) {
        ArrayList<EntityData> multiplied = new ArrayList<EntityData>();
        for (int i = 0; i < multipliers.size(); i++) {
            EntityData multiplierData = multipliers.get(i);

            List<EntityData> datas = message.getPayload();
            if (datas != null) {
                for (int j = 0; j < datas.size(); j++) {
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                    EntityData oldData = datas.get(j);
                    EntityData newData = new EntityData();
                    newData.putAll(oldData);
                    newData.putAll(multiplierData);
                    multiplied.add(newData);
                    if (multiplied.size() >= rowsPerMessage) {
                        callback.sendEntityDataMessage(null, multiplied);
                        multiplied = new ArrayList<EntityData>();
                    }
                }
            }
        }

        if (multiplied.size() > 0) {
            callback.sendEntityDataMessage(null, multiplied);
        }
    }

}
