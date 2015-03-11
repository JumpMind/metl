package org.jumpmind.symmetric.is.core.runtime.component;

public class ComponentStatistics {

	private int numberInboundMessages=0;
	private int numberOutboundMessages=0;
	
	public int getNumberInboundMessages() {
		return numberInboundMessages;
	}
	
	public void setNumberInboundMessages(int numberInboundMessages) {
		this.numberInboundMessages = numberInboundMessages;
	}
	
	public void incrementInboundMessages() {
		this.numberInboundMessages++;
	}
	
	public void setNumberOutboundMessages(int numberOutboundMessages) {
        this.numberOutboundMessages = numberOutboundMessages;
    }
	
	public int getNumberOutboundMessages() {
        return numberOutboundMessages;
    }
	
	public void incrementOutboundMessages() {
	    this.numberOutboundMessages++;
	}

}
