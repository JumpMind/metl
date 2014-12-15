package org.jumpmind.symmetric.is.core.runtime.component;

public class ComponentStatistics {

	private int numberInboundMessages=0;
	
	public int getNumberInboundMessages() {
		return numberInboundMessages;
	}
	public void setNumberInboundMessages(int numberInboundMessages) {
		this.numberInboundMessages = numberInboundMessages;
	}
	public void incrementInboundMessages() {
		this.numberInboundMessages++;
	}

}
