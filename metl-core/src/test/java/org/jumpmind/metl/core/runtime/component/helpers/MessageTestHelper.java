package org.jumpmind.metl.core.runtime.component.helpers;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.util.NameValue;

public class MessageTestHelper {
	public static Message buildMessageSingleEntityDataWithAttributes(String originatingStepId) {
		Message message = new Message(originatingStepId);
		message.setPayload(PayloadTestHelper.createPayloadWithMultipleEntityData());
		return message;
	}
	
	public static Message buildMessageSingleEntityData(String originatingStepId, NameValue... nameValues) {
		Message message = new Message(originatingStepId);
		ArrayList<EntityData> payload = new ArrayList<EntityData>();
		EntityData entityData = new EntityData();
		
		for (NameValue nv : nameValues) {
			entityData.put(nv.getName(), nv.getValue());
		}
		
		payload.add(entityData);
		message.setPayload(payload);
		return message;
	}
	
	public static Message buildMessagePayload(String originatingStepId, int rows, NameValue... nameValues) {
		Message message = new Message(originatingStepId);
		ArrayList<EntityData> payload = new ArrayList<EntityData>();
		EntityData entityData = new EntityData();
		
		for (NameValue nv : nameValues) {
			entityData.put(nv.getName(), nv.getValue());
		}
		
		payload.add(entityData);
		message.setPayload(payload);
		return message;
	}
}
