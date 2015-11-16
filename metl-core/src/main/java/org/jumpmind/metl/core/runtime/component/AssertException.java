package org.jumpmind.metl.core.runtime.component;

public class AssertException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public AssertException() {
    }

    public AssertException(String message) {
        super(message);
    }

    public AssertException(Throwable cause) {
        super(cause);
    }

    public AssertException(String message, Throwable cause) {
        super(message, cause);
    }

    public AssertException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
