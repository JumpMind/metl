package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.Http;
import org.jumpmind.symmetric.is.core.runtime.resource.HttpOutputStream;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceRuntime;
import org.jumpmind.symmetric.is.core.runtime.resource.IStreamable;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;

@ComponentDefinition(
        typeName = Web.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "web.png",
        inputMessage = MessageType.TEXT,
        outgoingMessage = MessageType.TEXT,
        resourceCategory = ResourceCategory.STREAMABLE)
public class Web extends AbstractComponentRuntime {

    public static final String TYPE = "Web";

    public static final String DEFAULT_CHARSET = "UTF-8";

    @SettingDefinition(order = 10, required = false, type = Type.TEXT, label = "Append To Url")
    public final static String RELATIVE_PATH = "relative.path";

    String relativePath;

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
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();

        IStreamable streamable = getResourceReference();

        ArrayList<String> outputPayload = new ArrayList<String>();
        ArrayList<String> inputPayload = inputMessage.getPayload();
        try {
            for (String requestContent : inputPayload) {
                getComponentStatistics().incrementNumberEntitiesProcessed();
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
                outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
                messageTarget.put(outputMessage);
            }
        } catch (IOException e) {
            throw new IoException(String.format("Error writing to %s ", streamable), e);
        }
    }

}
