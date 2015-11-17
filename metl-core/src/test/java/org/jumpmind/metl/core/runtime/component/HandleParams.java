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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class HandleParams {
    Message inputMessage;
    TestingSendMessageCallback callback;
    Boolean unitOfWorkLastMessage;

    public HandleParams() {
        this.inputMessage = new Message("inputMessage");
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

        @Override
        public void sendMessage(Map<String, Serializable> additionalHeaders, Serializable payload, String... targetStepIds) {
            if (payload instanceof List) {
                Message message = new Message("unitTest");
                message.setPayload(payload);
                monitor.getMessages().add(message);
            }

            Collections.addAll(monitor.getTargetStepIds(), targetStepIds);
        }

        @Override
        public void sendShutdownMessage(boolean cancel) {
            monitor.incrementShutdownMessageCount();
        }

        @Override
        public void sendControlMessage(Map<String, Serializable> messageHeaders) {
            monitor.incrementStartupMessageCount();
        }

        @Override
        public void forward(Message message) {
            if (!(message instanceof ControlMessage)) {
                sendMessage(message.getHeader(), message.getPayload());
            }
        };

        HandleMessageMonitor getMonitor() {
            return monitor;
        }
    }
}
