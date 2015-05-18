package org.jumpmind.symmetric.is.core.model;

public enum ExecutionStatus {

    DONE, ERROR, CANCELLED, ABANDONED, RUNNING, READY; 
    
    public static boolean isDone(ExecutionStatus status) {
        return status == DONE || status == CANCELLED || status == ERROR;
    }
    
}
