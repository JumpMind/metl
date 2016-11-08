package org.jumpmind.metl.sample;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class Sample extends AbstractComponentRuntime {

    public Sample() {
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
