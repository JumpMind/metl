package org.jumpmind.metl.core.runtime.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
		public void sendMessage(Serializable payload, boolean lastMessage, String... targetStepIds) {
			if (payload instanceof List) {
				monitor.getPayloads().add(payload);
			}
			
			Collections.addAll(monitor.getTargetStepIds(), targetStepIds);
			monitor.incrementSendMessageCount();
		}

		@Override
		public void sendShutdownMessage(boolean cancel) {
			monitor.incrementShutdownMessageCount();
		}

		@Override
		public void sendStartupMessage() {
			monitor.incrementStartupMessageCount();
		}

		HandleMessageMonitor getMonitor() {
			return monitor;
		}
	}
}
