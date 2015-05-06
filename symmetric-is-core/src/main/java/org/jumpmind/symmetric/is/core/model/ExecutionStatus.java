package org.jumpmind.symmetric.is.core.model;

public enum ExecutionStatus {

    DONE, ERROR, CANCELLED, RUNNING, READY, REQUESTED; 
    
    public static boolean isDone(ExecutionStatus status) {
        return status == DONE || status == CANCELLED || status == ERROR;
    }
    
}
