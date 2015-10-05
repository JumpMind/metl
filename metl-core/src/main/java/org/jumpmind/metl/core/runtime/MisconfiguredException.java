package org.jumpmind.metl.core.runtime;

public class MisconfiguredException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public MisconfiguredException() {
    }

    public MisconfiguredException(String message, Object ... args) {
        super(args != null ? String.format(message, args) : message);
    }

    public MisconfiguredException(Throwable cause) {
        super(cause);
    }

    public MisconfiguredException(String message, Throwable cause) {
        super(message, cause);
    }

    public MisconfiguredException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
