package org.jumpmind.metl.core.model;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public enum ExecutionStatus {

    DONE, ERROR, CANCELLED, ABANDONED, RUNNING, READY;

    public static boolean isDone(ExecutionStatus status) {
        return status == DONE || status == CANCELLED || status == ERROR || status == ABANDONED;
    }

    public static boolean isDone(String statusStr) {
        ExecutionStatus status = null;
        if (isNotBlank(statusStr)) {
            if (statusStr.lastIndexOf(' ') >= 0) {
                statusStr = statusStr.substring(statusStr.lastIndexOf(' ')+1);
            }
            status = ExecutionStatus.valueOf(statusStr);
        }
        return status == DONE || status == CANCELLED || status == ERROR || status == ABANDONED;
    }

}
