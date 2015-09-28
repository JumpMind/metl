package org.jumpmind.metl.core.runtime.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class TextReader extends AbstractComponentRuntime {

    public static final String TYPE = "Text Reader";

    public static final String SETTING_ROWS_PER_MESSAGE = "rows.per.message";

    public static final String SETTING_TEXT = "text";

    @Override
    protected void start() {
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();
        int numberMessages = 0;
        int linesInMessage = 0;
        int textRowsPerMessage = context.getFlowStep().getComponent().getInt(SETTING_ROWS_PER_MESSAGE, 1000);
        ArrayList<String> payload = new ArrayList<String>();

        BufferedReader reader = null;
        String currentLine;
        try {
            reader = new BufferedReader(new StringReader(context.getFlowStep().getComponent().get(SETTING_TEXT, "")));
            while ((currentLine = reader.readLine()) != null) {
                if (linesInMessage == textRowsPerMessage) {
                    initAndSendMessage(payload, messageTarget, ++numberMessages, false);
                    linesInMessage = 0;
                    payload = new ArrayList<String>();
                }
                getComponentStatistics().incrementNumberEntitiesProcessed();
                payload.add(currentLine);
                linesInMessage++;
            }
        } catch (IOException e) {
            throw new IoException("Error reading from file " + e.getMessage());
        } finally {
            IOUtils.closeQuietly(reader);
        }

        String unitOfWork = getUnitOfWork();
        initAndSendMessage(payload, messageTarget, ++numberMessages, unitOfWork.equalsIgnoreCase(UNIT_OF_WORK_INPUT_MESSAGE)
                || (unitOfWork.equalsIgnoreCase(UNIT_OF_WORK_FLOW) && unitOfWorkLastMessage));
    }

    private void initAndSendMessage(ArrayList<String> payload, IMessageTarget messageTarget, int numberMessages, boolean lastMessage) {
        Message message = new Message(getFlowStepId());
        message.getHeader().setSequenceNumber(numberMessages);
        message.getHeader().setUnitOfWorkLastMessage(lastMessage);
        message.setPayload(payload);
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(message);
    }
}
