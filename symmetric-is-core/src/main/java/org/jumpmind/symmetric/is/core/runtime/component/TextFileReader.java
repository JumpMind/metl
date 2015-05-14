package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IStreamable;
import org.jumpmind.symmetric.is.core.runtime.resource.LocalFile;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;
import org.jumpmind.util.FormatUtils;

@ComponentDefinition(
        typeName = TextFileReader.TYPE,
        category = ComponentCategory.READER,
        iconImage = "textfilereader.png",
        inputMessage = MessageType.TEXT,
        outgoingMessage = MessageType.TEXT,
        resourceCategory = ResourceCategory.STREAMABLE)
public class TextFileReader extends AbstractComponentRuntime {

    public static final String TYPE = "Text File Reader";

    @SettingDefinition(
            order = 5,
            type = Type.BOOLEAN,
            defaultValue = "false",
            label = "Get File Name From Message")
    public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";

    @SettingDefinition(order = 10, type = Type.TEXT, label = "File Path")
    public final static String SETTING_RELATIVE_PATH = "textfilereader.relative.path";

    @SettingDefinition(
            type = Type.BOOLEAN,
            order = 20,
            required = true,
            provided = true,
            defaultValue = "true",
            label = "Must Exist")
    public static final String SETTING_MUST_EXIST = "textfilereader.must.exist";

    @SettingDefinition(type = Type.INTEGER, order = 30, defaultValue = "1000", label = "Rows / Msg")
    public static final String SETTING_ROWS_PER_MESSAGE = "textfilereader.text.rows.per.message";

    @SettingDefinition(type = Type.INTEGER, order = 40, label = "Line Terminator")
    public static final String SETTING_HEADER_LINES_TO_SKIP = "textfilereader.text.header.lines.to.skip";

    @SettingDefinition(
            order = 50,
            type = Type.BOOLEAN,
            defaultValue = "false",
            label = "Delete On Complete")
    public final static String SETTING_DELETE_ON_COMPLETE = "delete.on.complete";

    @SettingDefinition(order = 60, type = Type.TEXT, label = "Encoding", defaultValue = "UTF-8")
    public final static String SETTING_ENCODING = "textfilereader.encoding";

    String relativePathAndFile;

    boolean mustExist;

    boolean getFileNameFromMessage = false;

    boolean deleteOnComplete = false;

    int textRowsPerMessage = 1000;

    int textHeaderLinesToSkip;

    String encoding = "UTF-8";

    @Override
    protected void start() {
        
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        String currentLine;
        int linesRead = 0;
        int linesInMessage = 0;
        int numberMessages = 0;
        List<String> files = new ArrayList<String>();
        if (getFileNameFromMessage) {
            List<String> fullyQualifiedFiles = inputMessage.getPayload();            
            String path = getResourceRuntime().getResource().get(LocalFile.LOCALFILE_PATH);
            for (String fullyQualifiedFile : fullyQualifiedFiles) {
                if (fullyQualifiedFile.startsWith(path)) {
                    files.add(fullyQualifiedFile.substring(path.length()));
                } else {
                    files.add(fullyQualifiedFile);
                }
            }
        } else {
            files.add(relativePathAndFile);
        }

        for (String file : files) {
            InputStream inStream = null;
            BufferedReader reader = null;
            try {
                IStreamable resource = (IStreamable)getResourceReference();
                String filePath = FormatUtils.replaceTokens(file, context.getFlowParametersAsString(), true);
                inStream = resource.getInputStream(filePath, mustExist);
                reader = new BufferedReader(new InputStreamReader(inStream, encoding));
                ArrayList<String> payload = new ArrayList<String>();
                while ((currentLine = reader.readLine()) != null) {
                    linesRead++;
                    if (linesRead > textHeaderLinesToSkip) {                        
                        if (linesInMessage >= textRowsPerMessage) {
                            initAndSendMessage(payload, inputMessage, messageTarget, numberMessages, false);
                            linesInMessage = 0;
                            payload = new ArrayList<String>();
                        }
                        getComponentStatistics().incrementNumberEntitiesProcessed();
                        payload.add(currentLine);
                        linesInMessage++;
                    }
                }
                initAndSendMessage(payload, inputMessage, messageTarget, numberMessages, true);
            } catch (IOException e) {
                throw new IoException("Error reading from file " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(reader);
                IOUtils.closeQuietly(inStream);
            }

        }

    }

    private void applySettings() {
        Component component = getComponent();
        relativePathAndFile = component.get(SETTING_RELATIVE_PATH, relativePathAndFile);
        mustExist = component.getBoolean(SETTING_MUST_EXIST, mustExist);
        textRowsPerMessage = component.getInt(SETTING_ROWS_PER_MESSAGE, textRowsPerMessage);
        textHeaderLinesToSkip = component.getInt(SETTING_HEADER_LINES_TO_SKIP,
                textHeaderLinesToSkip);
        textRowsPerMessage = component.getInt(SETTING_ROWS_PER_MESSAGE, textRowsPerMessage);
        getFileNameFromMessage = component.getBoolean(SETTING_GET_FILE_FROM_MESSAGE,
                getFileNameFromMessage);
        deleteOnComplete = component.getBoolean(SETTING_DELETE_ON_COMPLETE, deleteOnComplete);
        encoding = component.get(SETTING_ENCODING, encoding);
    }

    private void initAndSendMessage(ArrayList<String> payload, Message inputMessage, IMessageTarget messageTarget,
            int numberMessages, boolean lastMessage) {
        numberMessages++;        
        Message message = new Message(getFlowStepId()); 
        message.getHeader().setSequenceNumber(numberMessages);
        message.getHeader().setLastMessage(lastMessage);
        message.setPayload(payload);
        getComponentStatistics().incrementOutboundMessages();
        messageTarget.put(message);
    }

}
