package org.jumpmind.metl.core.runtime;

public class StartupMessage extends Message {

	private static final long serialVersionUID = 1L;

	public StartupMessage() {
	    this(null);
    }
	
	public StartupMessage(String originatingFlowStepId) {
	    super(originatingFlowStepId);
	    this.getHeader().setUnitOfWorkLastMessage(true);
    }
	
	
}
