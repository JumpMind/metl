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
        int linesInMessage = 0;
        int textRowsPerMessage = context.getFlowStep().getComponent().getInt(SETTING_ROWS_PER_MESSAGE, 1000);
        ArrayList<String> payload = new ArrayList<String>();

        BufferedReader reader = null;
        String currentLine;
        try {
            reader = new BufferedReader(new StringReader(context.getFlowStep().getComponent().get(SETTING_TEXT, "")));
            while ((currentLine = reader.readLine()) != null) {
                if (linesInMessage == textRowsPerMessage) {
                    sendMessage(payload, messageTarget, false);
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
        
        sendMessage(payload, messageTarget, unitOfWorkLastMessage);
    }

}
