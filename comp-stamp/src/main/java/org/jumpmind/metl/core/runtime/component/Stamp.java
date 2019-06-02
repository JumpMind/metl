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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.time.FastDateFormat;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Stamp extends AbstractComponentRuntime {

    public static final String HEADER_NAME_TO_USE = "header.name.to.use";
    public static final String ENTITY_COLUMN = "entity.column";
    public static final String STAMP_TYPE = "stamp.type";

    public static final String TYPE_FIRST_ENTITY_ATTRIBUTE = "FIRST ENTITY ATTRIBUTE";
    public static final String TYPE_TIMESTAMP = "TIMESTAMP";
    public static final String TYPE_TIMESTAMP_STRING_1 = "yyyy-MM-dd hh:mm:ss.S";
    public static final String TYPE_TIMESTAMP_STRING_2 = "yyyyMMddhhmmssS";

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        String stampType = properties.get(STAMP_TYPE);
        String messageHeaderKey = properties.get(HEADER_NAME_TO_USE);
        Serializable messageHeaderValue = null;
        if (TYPE_FIRST_ENTITY_ATTRIBUTE.equals(stampType) && inputMessage instanceof EntityDataMessage) {
            EntityDataMessage message = (EntityDataMessage) inputMessage;
            ArrayList<EntityData> payload = message.getPayload();
            String attributeId = properties.get(ENTITY_COLUMN);
            for (EntityData entityData : payload) {
                messageHeaderValue = (Serializable) entityData.get(attributeId);
                if (messageHeaderValue != null) {
                    break;
                }
            }
        } else if (TYPE_TIMESTAMP.equals(stampType)) {
            messageHeaderValue = new Date();
        } else if (TYPE_TIMESTAMP_STRING_1.equals(stampType)) {
            messageHeaderValue = FastDateFormat.getInstance(TYPE_TIMESTAMP_STRING_1).format(new Date());
        } else if (TYPE_TIMESTAMP_STRING_2.equals(stampType)) {
            messageHeaderValue = FastDateFormat.getInstance(TYPE_TIMESTAMP_STRING_2).format(new Date());
        }


        Map<String, Serializable> messageHeaders = new HashMap<>();
        messageHeaders.put(messageHeaderKey, messageHeaderValue);
        callback.forward(messageHeaders, inputMessage);

    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

}
