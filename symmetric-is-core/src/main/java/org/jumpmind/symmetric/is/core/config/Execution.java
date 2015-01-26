package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ExecutionData;

public class Execution extends AbstractObject<ExecutionData> {

    private static final long serialVersionUID = 1L;

    public void setExecutionStatus(ExecutionStatus status) {
        data.setStatus(status.name());
    }

    public ExecutionStatus getExecutionStatus() {
        return ExecutionStatus.valueOf(data.getStatus());
    }
}
