package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class HttpResponse extends AbstractHttpRequestResponse implements IHasResults {

    StringBuilder response;

    public HttpResponse() {
    }

    @Override
    public void start() {
        response = new StringBuilder();
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
        if (properties.is("returns.data")) {
            if (inputMessage instanceof ContentMessage) {
                ContentMessage<?> textMessage = (ContentMessage<?>) inputMessage;
                response.append(textMessage.getTextFromPayload());
            }
        }
    }

    @Override
    public Results getResults() {
        return new Results(getResponse(), getContentType());
    }

    private String getContentType() {
        if (response instanceof CharSequence) {
            // content type only means anything if we are providing the output
            // in string format
            return properties.get("content.type");
        } else {
            return null;
        }
    }

    private Object getResponse() {
        if (response instanceof CharSequence) {
            return response.toString();
        } else {
            return response;
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
