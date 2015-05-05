package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = XmlFormatter.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "xmlformatter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.TEXT)
public class XmlFormatter extends AbstractComponent {

    public static final String TYPE = "Format XML";

    TypedProperties properties;

    public final static String XML_FORMATTER_XPATH = "xml.formatter.xpath";
    
    public final static String XML_FORMATTER_TEMPLATE = "xml.formatter.template";

    @Override
    public void start(String executionId, IExecutionTracker executionTracker) {
        super.start(executionId, executionTracker);
    }

    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {
        componentStatistics.incrementInboundMessages();
        ArrayList<EntityData> inputRows = inputMessage.getPayload();

        Message outputMessage = new Message(flowStep.getId());
        ArrayList<String> outputPayload = new ArrayList<String>();

        for (EntityData inputRow : inputRows) {
            outputPayload.add(processInputRow(inputRow));
        }
        outputMessage.setPayload(outputPayload);
        executionTracker.log(executionId, LogLevel.INFO, this, outputPayload.toString());
        componentStatistics.incrementOutboundMessages();
        outputMessage.getHeader().setSequenceNumber(componentStatistics.getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
    }

    private String processInputRow(EntityData inputRow) {
        return "not ready yet";
    }

}
