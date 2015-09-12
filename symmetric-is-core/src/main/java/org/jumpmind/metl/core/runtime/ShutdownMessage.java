package org.jumpmind.symmetric.is.core.runtime;

public class ShutdownMessage extends Message {

	private static final long serialVersionUID = 1L;
	
	public boolean cancelled = false;

    public ShutdownMessage(String originatingStepId) {
        super(originatingStepId);
    }
    
    public ShutdownMessage(String originatingStepId, boolean cancelled) {
        super(originatingStepId);
        this.cancelled = cancelled;
    }

    
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
    
    public boolean isCancelled() {
        return cancelled;
    }
		
}
