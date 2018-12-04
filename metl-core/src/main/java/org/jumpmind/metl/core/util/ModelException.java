package org.jumpmind.metl.core.util;

public class ModelException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ModelException() {
        super();
    }

    public ModelException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ModelException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelException(String message) {
        super(message);
    }

    public ModelException(Throwable cause) {
        super(cause);
    }

}
