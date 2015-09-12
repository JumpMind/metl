package org.jumpmind.metl.core.runtime.component;

public class ComponentStatistics {

    private int numberInboundMessages = 0;
    private int numberOutboundMessages = 0;
    private int numberEntitiesProcessed = 0;

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

    public void setNumberEntitiesProcessed(int numberEntitiesProcessed) {
        this.numberEntitiesProcessed = numberEntitiesProcessed;
    }

    public int getNumberEntitiesProcessed() {
        return numberEntitiesProcessed;
    }

    public void incrementNumberEntitiesProcessed() {
        numberEntitiesProcessed++;
    }

    public void incrementNumberEntitiesProcessed(int count) {
        numberEntitiesProcessed += count;
    }

}
