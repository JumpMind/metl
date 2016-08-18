package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static org.jumpmind.metl.core.runtime.FlowConstants.REQUEST_VALUE_PARAMETER;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class HttpRequest extends AbstractHttpRequestResponse implements IHasSecurity {

    public static final String PATH = "path";

    public static final String HTTP_METHOD = "http.method";

    public static final String SECURITY_SCHEME = "security.scheme";

    public static final String SECURE_USERNAME = "secure.username";

    public static final String SECURE_PASSWORD = "secure.password";

    public HttpRequest() {
    }

    @Override
    public SecurityType getSecurityType() {
        return SecurityType.valueOf(properties.get(SECURITY_SCHEME, SecurityType.NONE.name()));
    }

    @Override
    public String getPassword() {
        return properties.get(SECURE_PASSWORD);
    }

    @Override
    public String getUsername() {
        return properties.get(SECURE_USERNAME);
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
        String requestPayload = getComponentContext().getFlowParameters()
                .get(REQUEST_VALUE_PARAMETER);
        if (isNotBlank(requestPayload)) {
            callback.sendTextMessage(inputMessage.getHeader(), requestPayload);
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

}
