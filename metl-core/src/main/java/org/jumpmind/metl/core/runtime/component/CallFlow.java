package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class CallFlow extends AbstractComponentRuntime {

    public final static String SETTING_FLOW_ID = "flow.id";

    String flowId;

    public CallFlow() {
    }

    @Override
    protected void start() {

    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {

    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

}
