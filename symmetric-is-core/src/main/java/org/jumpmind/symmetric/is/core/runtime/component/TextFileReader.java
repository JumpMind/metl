package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
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
    
    public static final String ACTION_NONE = "None";
    public static final String ACTION_DELETE = "Delete";
    public static final String ACTION_ARCHIVE = "Archive";

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

    @SettingDefinition(type = Type.INTEGER, order = 30, defaultValue = "10000", label = "Rows / Msg")
    public static final String SETTING_ROWS_PER_MESSAGE = "textfilereader.text.rows.per.message";

    @SettingDefinition(order = 35, type = Type.CHOICE, defaultValue = "NONE", choices = {
            ACTION_NONE, ACTION_ARCHIVE, ACTION_DELETE }, label = "Action on Success")
    public final static String SETTING_ACTION_ON_SUCCESS = "action.on.success";

    @SettingDefinition(order = 40, type = Type.TEXT, label = "Archive On Success Path")
    public final static String SETTING_ARCHIVE_ON_SUCCESS_PATH = "archive.on.success.path";

    @SettingDefinition(order = 45, type = Type.CHOICE, defaultValue = "NONE", choices = {
            ACTION_NONE, ACTION_ARCHIVE, ACTION_DELETE }, label = "Action on Error")
    public final static String SETTING_ACTION_ON_ERROR = "action.on.error";

    @SettingDefinition(order = 50, type = Type.TEXT, label = "Archive On Error Path")
    public final static String SETTING_ARCHIVE_ON_ERROR_PATH = "archive.on.error.path";

    @SettingDefinition(order = 60, type = Type.TEXT, label = "Encoding", defaultValue = "UTF-8")
    public final static String SETTING_ENCODING = "textfilereader.encoding";
    
    @SettingDefinition(type = Type.INTEGER, order = 70, label = "Header Lines to Skip")
    public static final String SETTING_HEADER_LINES_TO_SKIP = "textfilereader.text.header.lines.to.skip";


    String relativePathAndFile;

    boolean mustExist;

    boolean getFileNameFromMessage = false;

    String actionOnSuccess = ACTION_NONE;

    String archiveOnSuccessPath;

    String actionOnError = ACTION_NONE;

    String archiveOnErrorPath;

    int textRowsPerMessage = 10000;

    int textHeaderLinesToSkip;

    String encoding = "UTF-8";
    
    List<String> filesRead;

    @Override
    protected void start() {
        filesRead = new ArrayList<String>();
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        String currentLine;
        int linesRead = 0;
        int numberMessages = 0;
        List<String> files = new ArrayList<String>();
        if (getFileNameFromMessage) {
            List<String> fullyQualifiedFiles = inputMessage.getPayload();            
            String path = getResourceRuntime().getAgentOverrides().get(LocalFile.LOCALFILE_PATH);
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
        
        filesRead.addAll(files);

        for (String file : files) {
            InputStream inStream = null;
            BufferedReader reader = null;
            int linesInMessage = 0;
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
    
    @Override
    public void flowCompletedWithErrors(Throwable myError) {
        if (ACTION_ARCHIVE.equals(actionOnError)) {
            archive(archiveOnErrorPath);
        } else if (ACTION_DELETE.equals(actionOnError)) {
            deleteFiles();
        }
    }

    @Override
    public void flowCompleted() {
        if (ACTION_ARCHIVE.equals(actionOnSuccess)) {
            archive(archiveOnSuccessPath);
        } else if (ACTION_DELETE.equals(actionOnSuccess)) {
            deleteFiles();
        }
    }

    protected void deleteFiles() {
        IStreamable streamable = getResourceReference();
        for (String srcFile : filesRead) {
            if(streamable.delete(srcFile)) {
                log(LogLevel.INFO, "Deleted %s", srcFile);
            } else {
                log(LogLevel.WARN, "Failed to delete %s", srcFile);
            } 
        }
    }

    protected void archive(String archivePath) {
        String path = getResourceRuntime().getAgentOverrides().get(LocalFile.LOCALFILE_PATH);
        File destDir = new File(path, archivePath);
        for (String srcFile : filesRead) {
            try {
                log(LogLevel.INFO, "Archiving %s tp %s", srcFile, destDir.getAbsolutePath());
                FileUtils.moveFileToDirectory(new File(path, srcFile), destDir, true);
            } catch (IOException e) {
                throw new IoException(e);
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
        actionOnSuccess = component.get(SETTING_ACTION_ON_SUCCESS, actionOnSuccess);
        actionOnError = component.get(SETTING_ACTION_ON_ERROR, actionOnError);
        archiveOnErrorPath = FormatUtils.replaceTokens(
                component.get(SETTING_ARCHIVE_ON_ERROR_PATH), context.getFlowParametersAsString(),
                true);
        archiveOnSuccessPath = FormatUtils.replaceTokens(
                component.get(SETTING_ARCHIVE_ON_SUCCESS_PATH),
                context.getFlowParametersAsString(), true);
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
