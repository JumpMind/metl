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
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

/*
 * Testing-only implementation of ISendMessageCallback that allows for writing
 * assertions against how and with what a callback was invoked.
 */
class NoOpSendMessageCallback implements ISendMessageCallback {
    String invokedMethodName;
    Map<String, Serializable> messageHeaders;
    Object payload;
    String[] targetStepIds;
    Boolean cancel;
    Message message;

    @Override
    public void sendEntityDataMessage(Map<String, Serializable> messageHeaders,
            ArrayList<EntityData> payload, String... targetStepIds) {
        invokedMethodName = "sendEntityDataMessage";
        this.messageHeaders = messageHeaders;
        this.payload = payload;
        this.targetStepIds = targetStepIds;
    }

    @Override
    public void sendTextMessage(Map<String, Serializable> messageHeaders, ArrayList<String> payload,
            String... targetStepIds) {
        invokedMethodName = "sendTextMessage";
        this.messageHeaders = messageHeaders;
        this.payload = payload;
        this.targetStepIds = targetStepIds;
    }

    @Override
    public void sendTextMessage(Map<String, Serializable> messageHeaders, String payload,
            String... targetStepIds) {
        invokedMethodName = "sendTextMessage";
        this.messageHeaders = messageHeaders;
        this.payload = payload;
        this.targetStepIds = targetStepIds;
    }

    @Override
    public void sendBinaryMessage(Map<String, Serializable> messageHeaders, byte[] payload,
            String... targetStepIds) {
        invokedMethodName = "sendBinaryMessage";
        this.messageHeaders = messageHeaders;
        this.payload = payload;
        this.targetStepIds = targetStepIds;
    }

    @Override
    public void sendShutdownMessage(boolean cancel) {
        invokedMethodName = "sendShutdownMessage";
        this.cancel = cancel;
    }

    @Override
    public void sendControlMessage(Map<String, Serializable> messageHeaders,
            String... targetStepIds) {
        invokedMethodName = "sendControlMessage";
        this.messageHeaders = messageHeaders;
        this.targetStepIds = targetStepIds;
    }

    @Override
    public void sendControlMessage() {
        invokedMethodName = "sendControlMessage";
    }

    @Override
    public void forward(Message message) {
        invokedMethodName = "forward";
        this.message = message;
    }

    @Override
    public void forward(Map<String, Serializable> messageHeaders, Message message) {
        invokedMethodName = "forward";
        this.messageHeaders = messageHeaders;
        this.message = message;
    }

    @Override
    public void forwardMessageToErrorSuspense(Message message) {
        invokedMethodName = "forwardMessageToErrorSuspense";
        this.message = message;
    }
}
