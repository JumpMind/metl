package org.jumpmind.metl.core.authentication;

public class ConsoleAuthenticationCredentialException extends ConsoleAuthenticationException {

    private static final long serialVersionUID = 1L;
    
    public ConsoleAuthenticationCredentialException() {
    }

    public ConsoleAuthenticationCredentialException(Throwable cause) {
        super(cause);
    }

    public ConsoleAuthenticationCredentialException(String message, Object... args) {
        super(message, args);
    }

    public ConsoleAuthenticationCredentialException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConsoleAuthenticationCredentialException(String message, Throwable cause, Object... args) {
        super(message, cause, args);    }
}
