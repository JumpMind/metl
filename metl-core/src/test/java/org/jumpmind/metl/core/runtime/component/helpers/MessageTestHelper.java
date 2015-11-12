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
package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;
import java.util.Arrays;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntimeTestSupport;
import org.jumpmind.metl.core.runtime.component.HandleMessageMonitor;
import org.jumpmind.metl.core.runtime.component.HandleParams;
import org.jumpmind.metl.core.util.NameValue;

public class MessageTestHelper {

    public static Message nullMessage() {
        return null;
    }

    public static void addControlMessage(AbstractComponentRuntimeTestSupport testComponent, String originatinStepId,
            boolean unitOfWorkBoundaryReached) {

        ControlMessage message = new ControlMessage(originatinStepId);
        message.setPayload(new ArrayList<>());
        testComponent.getMessages().add(new HandleParams(message, unitOfWorkBoundaryReached));
    }

    public static void addInputMessage(AbstractComponentRuntimeTestSupport testComponent, boolean unitOfWorkLastMessage,
            boolean unitOfWorkBoundaryReached, String originatinStepId, EntityData... entities) {

        ArrayList<EntityData> payload = new ArrayList<EntityData>();

        for (EntityData d : entities) {
            payload.add(d);
        }

        Message message = new MessageBuilder(originatinStepId).withPayload(payload).build();
        testComponent.getMessages().add(new HandleParams(message, unitOfWorkBoundaryReached));
        if (unitOfWorkLastMessage) {
            testComponent.getMessages().add(new ControlMessage(originatinStepId));
        }

    }

    public static void addInputMessage(AbstractComponentRuntimeTestSupport testComponent, boolean unitOfWorkLastMessage,
            boolean unitOfWorkBoundaryReached, String originatinStepId, String... values) {

        ArrayList<String> payload = new ArrayList<String>();

        for (String s : values) {
            payload.add(s);
        }

        Message message = new MessageBuilder(originatinStepId).withPayloadString(payload).build();

        testComponent.getMessages().add(new HandleParams(message, unitOfWorkBoundaryReached));
        if (unitOfWorkLastMessage) {
            testComponent.getMessages().add(new ControlMessage(originatinStepId));
        }

    }

    public static void addInputMessage(AbstractComponentRuntimeTestSupport testComponent, boolean unitOfWorkLastMessage,
            boolean unitOfWorkBoundaryReached, String originatinStepId, String key, Object value) {

        Message message = new MessageBuilder(originatinStepId)
                .withPayload(new PayloadBuilder().withRow(new EntityDataBuilder().withKV(key, value).build()).buildED()).build();

        testComponent.getMessages().add(new HandleParams(message, unitOfWorkBoundaryReached));
        if (unitOfWorkLastMessage) {
            testComponent.getMessages().add(new ControlMessage(originatinStepId));
        }

    }

    public static void addOutputMonitor(AbstractComponentRuntimeTestSupport testComponent, Message... messages) {
        testComponent.getExpectedMonitors().add(getMessageMonitor(false, messages));
    }

    public static void addOutputMonitor(AbstractComponentRuntimeTestSupport testComponent, boolean xmlPayload, Message... messages) {
        testComponent.getExpectedMonitors().add(getMessageMonitor(xmlPayload, messages));
    }

    public static void addOutputMonitor(AbstractComponentRuntimeTestSupport testComponent, String key, Object value) {
        Message message = new MessageBuilder().withKeyValue(key, value).build();
        testComponent.getExpectedMonitors().add(getMessageMonitor(false, message));
    }

    public static void addOutputMonitor(AbstractComponentRuntimeTestSupport testComponent, String value) {
        Message message = new MessageBuilder().withValue(value).build();
        testComponent.getExpectedMonitors().add(getMessageMonitor(false, message));
    }

    public static void addOutputMonitor(AbstractComponentRuntimeTestSupport testComponent, boolean xmlPayload, String value) {
        Message message = new MessageBuilder().withValue(value).build();
        testComponent.getExpectedMonitors().add(getMessageMonitor(xmlPayload, message));
    }

    public static void addOutputMonitor(AbstractComponentRuntimeTestSupport testComponent, int startMessageCount, int shutdownMessageCount) {

        testComponent.getExpectedMonitors().add(getMessageMonitor(startMessageCount, shutdownMessageCount));
    }

    public static HandleMessageMonitor getMessageMonitor(boolean xmlPayload, Message... messages) {
        HandleMessageMonitor m = new HandleMessageMonitor();
        m.setXmlPayload(xmlPayload);
        if (messages == null || messages[0] == null) {
            m.setMessages(new ArrayList<Message>());
        } else {
            m.setMessages(Arrays.asList(messages));
        }
        return m;
    }

    public static HandleMessageMonitor getMessageMonitor(int startMessageCount, int shutdownMessageCount) {
        HandleMessageMonitor m = new HandleMessageMonitor();
        m.setStartupMessageCount(startMessageCount);
        m.setShutdownMessageCount(shutdownMessageCount);
        return m;
    }

    /*
     * public static Message buildMessageSingleEntityDataWithAttributes(String
     * originatingStepId) { Message message = new Message(originatingStepId);
     * message.setPayload(PayloadTestHelper.createPayloadWithMultipleEntityData(
     * )); return message; }
     * 
     * 
     * 
     * public static Message buildMessageSingleEntityData(String
     * originatingStepId, NameValue... nameValues) { Message message = new
     * Message(originatingStepId); ArrayList<EntityData> payload = new
     * ArrayList<EntityData>(); EntityData entityData = new EntityData();
     * 
     * for (NameValue nv : nameValues) { entityData.put(nv.getName(),
     * nv.getValue()); }
     * 
     * payload.add(entityData); message.setPayload(payload); return message; }
     * 
     * public static Message buildMessagePayload(String originatingStepId, int
     * rows, NameValue... nameValues) { Message message = new
     * Message(originatingStepId); ArrayList<EntityData> payload = new
     * ArrayList<EntityData>(); EntityData entityData = new EntityData();
     * 
     * for (NameValue nv : nameValues) { entityData.put(nv.getName(),
     * nv.getValue()); }
     * 
     * payload.add(entityData); message.setPayload(payload); return message; }
     */
}
