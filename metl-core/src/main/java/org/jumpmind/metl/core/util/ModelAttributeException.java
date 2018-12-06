package org.jumpmind.metl.core.util;

public class ModelAttributeException extends ModelException {

    private static final long serialVersionUID = 1L;

    public ModelAttributeException() {
        super();
    }

    public ModelAttributeException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ModelAttributeException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelAttributeException(String message) {
        super(message);
    }

    public ModelAttributeException(Throwable cause) {
        super(cause);
    }

}

