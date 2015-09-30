package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class HandleParams {
	Message inputMessage;
	MessageTarget target;
	Boolean unitOfWorkLastMessage;
	
	public HandleParams() {
		this.inputMessage = new Message("inputMessage");
		this.target = new MessageTarget();
		this.unitOfWorkLastMessage = false;
	}
	
	public HandleParams(Message inputMessage) {
		this.inputMessage = inputMessage;
		this.target = new MessageTarget();
		this.unitOfWorkLastMessage = false;
	}
	
	public HandleParams(Message inputMessage, boolean unitOfWorkLastMessage) {
		this.inputMessage = inputMessage;
		this.target = new MessageTarget();
		this.unitOfWorkLastMessage = unitOfWorkLastMessage;
	}
	
	Message getInputMessage() {
		return inputMessage;
	}
	void setInputMessage(Message inputMessage) {
		this.inputMessage = inputMessage;
	}
	MessageTarget getTarget() {
		return target;
	}
	void setTarget(MessageTarget target) {
		this.target = target;
	}
	Boolean getUnitOfWorkLastMessage() {
		return unitOfWorkLastMessage;
	}
	void setUnitOfWorkLastMessage(Boolean unitOfWorkLastMessage) {
		this.unitOfWorkLastMessage = unitOfWorkLastMessage;
	}
	
	
	class MessageTarget implements IMessageTarget {

        List<Message> targetMsgArray = new ArrayList<Message>();

        @Override
        public void put(Message message) {
            targetMsgArray.add(message);
        }

        public Message getMessage(int idx) {
            return targetMsgArray.get(idx);
        }

        public int getTargetMessageCount() {
            return targetMsgArray.size();
        }
    }
}
