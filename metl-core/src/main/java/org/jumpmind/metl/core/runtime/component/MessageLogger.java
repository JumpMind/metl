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

import java.io.Serializable;
import java.util.List;

import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MessageHeader;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class MessageLogger extends AbstractComponentRuntime {

    public static final String TYPE = "Message Logger";

    public static String SETTING_QUALIFY_WITH_ENTITY_NAME = "qualify.with.entity.name";

    @Override
    public void start() {
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        MessageHeader header = inputMessage.getHeader();
        log(LogLevel.INFO,
                String.format("%s{sequenceNumber=%d,unitOfWorkBoundaryReached=%s,source='%s',headers=%s}",
                        inputMessage.getClass().getSimpleName(), header.getSequenceNumber(), unitOfWorkBoundaryReached,
                        getFlow().findFlowStepWithId(header.getOriginatingStepId()).getName(), header.toString()));
        if (inputMessage instanceof ContentMessage<?>) {
            Serializable payload = ((ContentMessage<?>) inputMessage).getPayload();
            if (payload instanceof List) {
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) payload;
                for (Object object : list) {
                    if (object instanceof EntityData && getComponent().getInputModel() != null) {
                        getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                        log(LogLevel.INFO, String.format("Message Payload: %s", getComponent().toRow((EntityData) object,
                                context.getFlowStep().getComponent().getBoolean(SETTING_QUALIFY_WITH_ENTITY_NAME, true), true)));
                    } else {
                        getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                        log(LogLevel.INFO, String.format("Message Payload: %s", object));
                    }
                }
            }
        }

        callback.forward(inputMessage);
    }

}
