package org.jumpmind.metl.core.authentication;

public class ConsoleAuthenticationConnectionException extends ConsoleAuthenticationException {
    
    private static final long serialVersionUID = 1L;
    
    public ConsoleAuthenticationConnectionException() {
    }

    public ConsoleAuthenticationConnectionException(Throwable cause) {
        super(cause);
    }

    public ConsoleAuthenticationConnectionException(String message, Object... args) {
        super(message, args);
    }

    public ConsoleAuthenticationConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConsoleAuthenticationConnectionException(String message, Throwable cause, Object... args) {
        super(message, cause, args);    
    }
    
}
