package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;

import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionCategory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.localfile.IStreamableConnection;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(typeName = FileReaderComponent.TYPE, category = ComponentCategory.READER,
        supports = { ComponentSupports.OUTPUT_MESSAGE },
        connectionCategory = ConnectionCategory.RESOURCE)
public class FileReaderComponent extends AbstractComponent {

    public static final String TYPE = "File Reader";

    //TODO: THIS SHOULD PROBABLY BE A SETTING
    public static final String DEFAULT_CHARSET = Charset.defaultCharset().name();

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Path and File")
    public final static String FILEREADER_RELATIVE_PATH = "filereader.relative.path";

    @SettingDefinition(order = 20, type = Type.CHOICE,
            choices = { FileType.TEXT, FileType.BINARY }, defaultValue = FileType.TEXT,
            label = "File Type")
    public final static String FILEREADER_FILE_TYPE = "filereader.file.type";

    @SettingDefinition(type = Type.BOOLEAN, order = 30, required = true, provided = true,
            defaultValue = "true", label = "Must Exist")
    public static final String FILEREADER_MUST_EXIST = "filereader.must.exist";

    @SettingDefinition(type = Type.INTEGER, order = 40, defaultValue = "1",
            label = "Binary Size / Msg (KB)")
    public static final String FILEREADER_BINARY_SIZE_PER_MESSAGE = "filereader.binary.size.per.message";

    @SettingDefinition(type = Type.INTEGER, order = 50, defaultValue = "1000",
            label = "Text Rows / Msg")
    public static final String FILEREADER_TEXT_ROWS_PER_MESSAGE = "filereader.text.rows.per.message";

    @SettingDefinition(type = Type.INTEGER, order = 70, label = "Line Terminator")
    public static final String FILEREADER_TEXT_HEADER_LINES_TO_SKIP = "filereader.text.header.lines.to.skip";

    /* settings */
    String relativePathAndFile;
    String fileType;
    boolean mustExist;
    int binarySizePerMessage;
    int textRowsPerMessage;
    int textHeaderLinesToSkip;

    /* other vars */
    TypedProperties properties;
    InputStream inStream = null;
    BufferedReader reader = null;
    String encoding = DEFAULT_CHARSET;

    @Override
    public void start(IExecutionTracker executionTracker, IConnectionFactory connectionFactory) {
        super.start(executionTracker, connectionFactory);
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        if (fileType.equalsIgnoreCase(FileType.BINARY)) {
            handleBinaryFile(inputMessage, messageTarget);
        } else {
            handleTextFile(inputMessage, messageTarget);
        }
    }

    private void applySettings() {
        properties = componentNode.getComponentVersion().toTypedProperties(this, false);
        relativePathAndFile = properties.get(FILEREADER_RELATIVE_PATH);
        fileType = properties.get(FILEREADER_FILE_TYPE);
        mustExist = properties.is(FILEREADER_MUST_EXIST);
        binarySizePerMessage = properties.getInt(FILEREADER_BINARY_SIZE_PER_MESSAGE);
        textRowsPerMessage = properties.getInt(FILEREADER_TEXT_ROWS_PER_MESSAGE);
        textHeaderLinesToSkip = properties.getInt(FILEREADER_TEXT_HEADER_LINES_TO_SKIP);
    }

    private void handleBinaryFile(Message inputMessage, IMessageTarget messageTarget) {
        Message message = null;
        ByteBuffer buffer = ByteBuffer.allocate(binarySizePerMessage * 1024);
        open();
        try {
            int bytesRead = 0;
            //todo: set header variables for 1 of 2, 2 of 2, etc.
            while ((bytesRead = inStream.read(buffer.array())) != -1) {
                message = new Message(componentNode.getId());

                if (bytesRead == binarySizePerMessage * 1024) {
                    message.setPayload(buffer.array().clone());
                } else {
                    message.setPayload(trimByteArray(buffer.array(), bytesRead));
                }
                messageTarget.put(message);
                buffer.clear();
            }
        } catch (IOException e) {
            throw new IoException("Error reading from file " + e.getMessage());
        } finally {
            close();
        }
    }

    private byte[] trimByteArray(byte[] byteArray, int size) {
        byte[] newArray = new byte[size];
        System.arraycopy(byteArray, 0, newArray, 0, size);
        return newArray;
    }

    private void handleTextFile(Message inputMessage, IMessageTarget messageTarget) {
        String currentLine;
        int linesRead = 0;
        int linesInMessage = 0;
        open();
        try {
            reader = new BufferedReader(new InputStreamReader(inStream, encoding));
            ArrayList<String> payload = new ArrayList<String>();
            while ((currentLine = reader.readLine()) != null) {
                linesRead++;
                if (linesRead > textHeaderLinesToSkip) {
                    if (linesInMessage >= textRowsPerMessage) {
                        initAndSendMessage(payload, messageTarget, linesInMessage);
                    }
                    payload.add(currentLine);
                    linesInMessage++;
                }
            }
            if (linesInMessage > 0) {
                initAndSendMessage(payload, messageTarget, linesInMessage);
            }
        } catch (IOException e) {
            throw new IoException("Error reading from file " + e.getMessage());
        } finally {
            close();
        }
    }

    private void initAndSendMessage(ArrayList<String> payload, IMessageTarget messageTarget,
            int linesInMessage) {
        Message message = new Message(componentNode.getId());
        message.setPayload(new ArrayList<String>(payload));
        messageTarget.put(message);
        payload.clear();
        linesInMessage = 0;
    }

    private void open() {
        IStreamableConnection connection = (IStreamableConnection) this.connection.reference();
        connection.appendPath(relativePathAndFile, mustExist);
        inStream = connection.getInputStream();
    }

    protected void close() {
        try {
            if (inStream != null) {
                inStream.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            throw new IoException("Failure in closing the reader " + e.getMessage());
        }
    }
}
