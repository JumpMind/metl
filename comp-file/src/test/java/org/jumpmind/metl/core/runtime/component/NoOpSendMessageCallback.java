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
