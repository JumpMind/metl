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
import java.util.Collections;
import java.util.Map;

import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class HandleParams {
    Message inputMessage;
    TestingSendMessageCallback callback;
    Boolean unitOfWorkLastMessage;

    public HandleParams() {
        this.callback = new TestingSendMessageCallback();
        this.unitOfWorkLastMessage = false;
    }

    public HandleParams(Message inputMessage) {
        this.inputMessage = inputMessage;
        this.callback = new TestingSendMessageCallback();
        this.unitOfWorkLastMessage = false;
    }

    public HandleParams(Message inputMessage, boolean unitOfWorkLastMessage) {
        this.inputMessage = inputMessage;
        this.callback = new TestingSendMessageCallback();
        this.unitOfWorkLastMessage = unitOfWorkLastMessage;
    }

    Message getInputMessage() {
        return inputMessage;
    }

    void setInputMessage(Message inputMessage) {
        this.inputMessage = inputMessage;
    }

    TestingSendMessageCallback getCallback() {
        return callback;
    }

    void setTarget(TestingSendMessageCallback callback) {
        this.callback = callback;
    }

    Boolean getUnitOfWorkLastMessage() {
        return unitOfWorkLastMessage;
    }

    void setUnitOfWorkLastMessage(Boolean unitOfWorkLastMessage) {
        this.unitOfWorkLastMessage = unitOfWorkLastMessage;
    }

    public class TestingSendMessageCallback implements ISendMessageCallback {
        HandleMessageMonitor monitor = new HandleMessageMonitor();

        public void forward(Map<String, Serializable> messageHeaders, Message message) {
            if (message instanceof EntityDataMessage) {
                sendEntityDataMessage(messageHeaders, ((EntityDataMessage) message).getPayload());
            } else if (message instanceof TextMessage) {
                sendTextMessage(messageHeaders, ((TextMessage) message).getPayload());
            } else if (message instanceof BinaryMessage) {
                sendBinaryMessage(messageHeaders, ((BinaryMessage) message).getPayload());
            }
        }        
        
        @Override
        public void forward(Message message) {
            forward(null, message);
        }
        
        @Override
        public void sendBinaryMessage(Map<String, Serializable> messageHeaders, byte[] payload, String... targetStepIds) {
            BinaryMessage message = new BinaryMessage("unitTest");
            message.setPayload(payload);
            monitor.getMessages().add(message);
        Collections.addAll(monitor.getTargetStepIds(), targetStepIds);
        }
        
        @Override
        public void sendEntityDataMessage(Map<String, Serializable> messageHeaders, ArrayList<EntityData> payload, String... targetStepIds) {
            EntityDataMessage message = new EntityDataMessage("unitTest");
            message.setPayload(payload);
            monitor.getMessages().add(message);
        Collections.addAll(monitor.getTargetStepIds(), targetStepIds);
        }
        
        @Override
        public void sendTextMessage(Map<String, Serializable> messageHeaders, ArrayList<String> payload, String... targetStepIds) {
                TextMessage message = new TextMessage("unitTest");
                message.setPayload(payload);
                monitor.getMessages().add(message);
            Collections.addAll(monitor.getTargetStepIds(), targetStepIds);

        }
        
        @Override
        public void sendShutdownMessage(boolean cancel) {
            monitor.incrementShutdownMessageCount();
        }

        @Override
        public void sendControlMessage() {
            sendControlMessage(null);
        }

        @Override
        public void sendControlMessage(Map<String, Serializable> messageHeaders, String ... targetStepIds) {
            monitor.incrementStartupMessageCount();
        }

        HandleMessageMonitor getMonitor() {
            return monitor;
        }
    }
}
