package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Pgp extends AbstractComponentRuntime {

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean supportsStartupMessages() {
        // TODO Auto-generated method stub
        return false;
    }

}
