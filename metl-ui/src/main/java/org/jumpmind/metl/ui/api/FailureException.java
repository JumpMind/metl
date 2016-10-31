package org.jumpmind.metl.ui.api;

public class FailureException extends RuntimeException {

    
    private static final long serialVersionUID = 1L;
    
    private ExecutionResults executionResults;
    
    public FailureException(ExecutionResults results) {
        this.executionResults = results;
    }
    
    public ExecutionResults getExecutionResults() {
        return executionResults;
    }

}
