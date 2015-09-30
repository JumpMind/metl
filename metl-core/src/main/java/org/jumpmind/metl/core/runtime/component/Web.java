package org.jumpmind.metl.core.runtime.component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;
import org.jumpmind.metl.core.runtime.resource.Http;
import org.jumpmind.metl.core.runtime.resource.HttpOutputStream;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.IStreamable;
import org.jumpmind.util.FormatUtils;

public class Web extends AbstractComponentRuntime {

    public static final String TYPE = "Web";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public static final String RELATIVE_PATH = "relative.path";
    
    public static final String BODY_FROM = "body.from";
    
    public static final String BODY_TEXT = "body.text";
    
    public static final String PARAMETER_REPLACEMENT = "parameter.replacement";

    String relativePath;
    
    String bodyFrom;
    
    String bodyText;
    
    boolean parameterReplacement;
    
    String unitOfWork;

    @Override
    protected void start() {
        IResourceRuntime httpResource = getResourceRuntime();
        if (httpResource == null || !(httpResource instanceof Http)) {
            throw new IllegalStateException(String.format(
                    "A target resource of type %s must be chosen.  Please choose a resource.",
                    Http.TYPE));
        }

        Component component = getComponent();
        relativePath = component.get(RELATIVE_PATH);
        bodyFrom = component.get(BODY_FROM, "Message");
        bodyText = component.get(BODY_TEXT);
        parameterReplacement = component.getBoolean(PARAMETER_REPLACEMENT, false);
        unitOfWork = component.get(UNIT_OF_WORK, UNIT_OF_WORK_FLOW);
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();

        IStreamable streamable = getResourceReference();

        ArrayList<String> outputPayload = new ArrayList<String>();
        ArrayList<String> inputPayload = new ArrayList<String>();
        if (bodyFrom.equals("Message")) {
            inputPayload = inputMessage.getPayload();
        } else {
            inputPayload.add(bodyText);
        }
        try {
            for (String requestContent : inputPayload) {
                getComponentStatistics().incrementNumberEntitiesProcessed();
                if (parameterReplacement) {
                    requestContent = FormatUtils.replaceTokens(requestContent, context.getFlowParametersAsString(), true);
                }
                HttpOutputStream os = (HttpOutputStream) streamable.getOutputStream(relativePath,
                        false);
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os,
                        DEFAULT_CHARSET));
                try {
                    writer.write(requestContent);
                } finally {
                    writer.close();
                    String response = os.getResponse();
                    if (response != null) {
                        outputPayload.add(response);
                    }
                }
            }
            
            if (outputPayload.size() > 0) {
                Message outputMessage = new Message(getFlowStepId());
                outputMessage.setPayload(outputPayload);
                outputMessage.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());
                
                if (unitOfWork.equalsIgnoreCase(UNIT_OF_WORK_INPUT_MESSAGE) ||
                		(unitOfWork.equalsIgnoreCase(UNIT_OF_WORK_FLOW) && unitOfWorkLastMessage)) {
                    outputMessage.getHeader().setUnitOfWorkLastMessage(true);    
                }                    
                messageTarget.put(outputMessage);
            }
        } catch (IOException e) {
            throw new IoException(String.format("Error writing to %s ", streamable), e);
        }
    }

}
