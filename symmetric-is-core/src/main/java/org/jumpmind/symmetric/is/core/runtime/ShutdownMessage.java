package org.jumpmind.symmetric.is.core.runtime;

public class ShutdownMessage extends Message {

	private static final long serialVersionUID = 1L;

    public ShutdownMessage(String originatingNodeId) {
        super(originatingNodeId);
    }
		
}
