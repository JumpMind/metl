package org.jumpmind.metl.ui.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value=HttpStatus.PRECONDITION_REQUIRED)
public class CallToDesignTimeAgentException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CallToDesignTimeAgentException(String msg) {
        super(msg);
    }

}
