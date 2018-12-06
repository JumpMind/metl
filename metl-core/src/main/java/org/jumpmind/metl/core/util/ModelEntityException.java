package org.jumpmind.metl.core.util;

public class ModelEntityException extends ModelException {

    private static final long serialVersionUID = 1L;

    public ModelEntityException() {
        super();
    }

    public ModelEntityException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public ModelEntityException(String message, Throwable cause) {
        super(message, cause);
    }

    public ModelEntityException(String message) {
        super(message);
    }

    public ModelEntityException(Throwable cause) {
        super(cause);
    }

}

