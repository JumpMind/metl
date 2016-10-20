package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class HttpResponse extends AbstractHttpRequestResponse implements IHasResults {

    StringBuilder response;

    String detectedFormat;

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
                detectedFormat = (String)textMessage.getHeader().get(AbstractSerializer.FORMAT);
            }
        }
    }

    @Override
    public Results getResults() {
        return new Results(getResponse(), getContentType());
    }

    private String getContentType() {
        String contentType = properties.get("content.type");
        if (isBlank(contentType)) {
            if (isNotBlank(detectedFormat)) {
                if (AbstractSerializer.FORMAT_JSON.equals(detectedFormat)) {
                    contentType = "application/json;charset=utf-8";
                } else if (AbstractSerializer.FORMAT_XML.equals(detectedFormat)) {
                    contentType = "application/xml;charset=utf-8";
                }
            }
        }
        return contentType;
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
