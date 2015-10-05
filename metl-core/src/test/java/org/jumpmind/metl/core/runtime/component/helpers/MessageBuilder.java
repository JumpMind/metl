package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;

public class MessageBuilder {
	
	Message message;
	
	public MessageBuilder(String originatingStepId) {
		message = new Message(originatingStepId);
	}
	
	public MessageBuilder setPayload(ArrayList<EntityData> payload) {
		this.message.setPayload(payload);
		return this;
	}
	
	public MessageBuilder setPayloadString(ArrayList<String> payload) {
		this.message.setPayload(payload);
		return this;
	}
	
	public Message build() {
		return this.message;
	}

}
