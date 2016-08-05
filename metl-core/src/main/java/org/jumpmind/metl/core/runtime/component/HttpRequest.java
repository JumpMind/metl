package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class HttpRequest extends AbstractComponentRuntime {
    
    public static final String REQUEST_PAYLOAD = HttpRequest.class.getName() + ".REQUEST_PAYLOAD";
    
    public static final String CONTENT_TYPE = HttpRequest.class.getName() + ".CONTENT_TYPE";
    
    public static final String PATH = "path";
    
    public static final String HTTP_METHOD = "http.method";

    public HttpRequest() {
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        String requestPayload = getComponentContext().getFlowParameters().get(REQUEST_PAYLOAD);
        
        if (isNotBlank(requestPayload)) {
            //TODO: today this is never executed on a get method because the REQUEST_PAYLOAD parameter is never set.
            //should we always send a text message to the next component?
            ArrayList<String> payload = new ArrayList<>(1);
            payload.add(requestPayload);
            callback.sendTextMessage(inputMessage.getHeader(), payload);
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    protected void start() {
    }

}
