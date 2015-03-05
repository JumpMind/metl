package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

@ComponentDefinition(typeName = TextFileReader.TYPE, category = ComponentCategory.READER,
        supports = { ComponentSupports.OUTPUT_MESSAGE },
        connectionCategory = ConnectionCategory.RESOURCE)
public class TextFileReader extends AbstractComponent {

    public static final String TYPE = "Text File Reader";

    public static final String DEFAULT_CHARSET = Charset.defaultCharset().name();

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Path and File")
    public final static String TEXTFILEREADER_RELATIVE_PATH = "textfilereader.relative.path";

    @SettingDefinition(type = Type.BOOLEAN, order = 20, required = true, provided = true,
            defaultValue = "true", label = "Must Exist")
    public static final String TEXTFILEREADER_MUST_EXIST = "textfilereader.must.exist";

    @SettingDefinition(type = Type.INTEGER, order = 30, defaultValue = "1000",
            label = "Rows / Msg")
    public static final String TEXTFILEREADER_ROWS_PER_MESSAGE = "textfilereader.text.rows.per.message";

    @SettingDefinition(type = Type.INTEGER, order = 40, label = "Line Terminator")
    public static final String TEXTFILEREADER_HEADER_LINES_TO_SKIP = "textfilereader.text.header.lines.to.skip";

    /* settings */
    String relativePathAndFile;
    boolean mustExist;
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

    private void applySettings() {
        properties = componentNode.getComponentVersion().toTypedProperties(this, false);
        relativePathAndFile = properties.get(TEXTFILEREADER_RELATIVE_PATH);
        mustExist = properties.is(TEXTFILEREADER_MUST_EXIST);
        textRowsPerMessage = properties.getInt(TEXTFILEREADER_ROWS_PER_MESSAGE);
        textHeaderLinesToSkip = properties.getInt(TEXTFILEREADER_HEADER_LINES_TO_SKIP);
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
