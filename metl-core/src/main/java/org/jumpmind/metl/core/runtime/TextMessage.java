package org.jumpmind.metl.core.runtime;

import java.util.ArrayList;

public class TextMessage extends ContentMessage<ArrayList<String>> {

    private static final long serialVersionUID = 1L;

    public TextMessage(String originatingStepId) {
        super(originatingStepId);
    }

    public TextMessage(String originatingStepId, ArrayList<String> payload) {
        super(originatingStepId, payload);
    }

    public TextMessage addString(String value) {
        if (payload == null) {
            this.payload = new ArrayList<>();
        }
        this.payload.add(value);
        return this;
    }

}
