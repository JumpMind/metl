package org.jumpmind.metl.core.authentication;

public class ConsoleAuthenticationException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public ConsoleAuthenticationException() {
    }

    public ConsoleAuthenticationException(Throwable cause) {
        super(cause);
    }

    public ConsoleAuthenticationException(String message, Object... args) {
        super(String.format(message, args));
    }

    public ConsoleAuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConsoleAuthenticationException(String message, Throwable cause, Object... args) {
        super(String.format(message, args), cause); 
    }
}