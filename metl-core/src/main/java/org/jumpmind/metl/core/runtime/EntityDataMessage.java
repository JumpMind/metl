package org.jumpmind.metl.core.runtime;

import java.util.ArrayList;

public class EntityDataMessage extends ContentMessage<ArrayList<EntityData>> {

    private static final long serialVersionUID = 1L;

    public EntityDataMessage(String originatingStepId) {
        super(originatingStepId);
    }

    public EntityDataMessage(String originatingStepId, ArrayList<EntityData> payload) {
        super(originatingStepId, payload);
    }

}
