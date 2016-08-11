package org.jumpmind.metl.core.runtime.component;

abstract public class AbstractHttpRequestResponse extends AbstractComponentRuntime {

    public static enum PayloadFormat {BY_TABLE, BY_INBOUND_ROW}
    
    public static final String PAYLOAD_FORMAT = "payload.format";

    String payloadFormat;
    
    protected void init() {
        payloadFormat = properties.get(PAYLOAD_FORMAT, PayloadFormat.BY_INBOUND_ROW.name());
    }

}
