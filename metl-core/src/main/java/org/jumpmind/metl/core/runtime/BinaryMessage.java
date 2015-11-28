package org.jumpmind.metl.core.runtime;

public class BinaryMessage extends ContentMessage<byte[]> {

    private static final long serialVersionUID = 1L;

    public BinaryMessage(String originatingStepId) {
        super(originatingStepId);
    }

    public BinaryMessage(String originatingStepId, byte[] payload) {
        super(originatingStepId, payload);
    }

}
