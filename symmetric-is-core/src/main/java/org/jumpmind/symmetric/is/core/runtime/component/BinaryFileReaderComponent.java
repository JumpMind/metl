package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

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

@ComponentDefinition(typeName = BinaryFileReaderComponent.TYPE, category = ComponentCategory.READER,
        supports = { ComponentSupports.OUTPUT_MESSAGE },
        connectionCategory = ConnectionCategory.RESOURCE)
public class BinaryFileReaderComponent extends AbstractComponent {

    public static final String TYPE = "Binary File Reader";

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Path and File")
    public final static String BINARYFILEREADER_RELATIVE_PATH = "binaryfilereader.relative.path";

    @SettingDefinition(type = Type.BOOLEAN, order = 20, required = true, provided = true,
            defaultValue = "true", label = "Must Exist")
    public static final String BINARYFILEREADER_MUST_EXIST = "binaryfilereader.must.exist";

    @SettingDefinition(type = Type.INTEGER, order = 30, defaultValue = "7",
            label = "Size / Msg (KB)")
    public static final String BINARYFILEREADER_SIZE_PER_MESSAGE = "binaryfilereader.size.per.message";

    /* settings */
    String relativePathAndFile;
    boolean mustExist;
    int sizePerMessage;

    /* other vars */
    TypedProperties properties;
    InputStream inStream = null;

    @Override
    public void start(IExecutionTracker executionTracker, IConnectionFactory connectionFactory) {
        super.start(executionTracker, connectionFactory);
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        Message message = null;
        ByteBuffer buffer = ByteBuffer.allocate(sizePerMessage * 1024);
        open();
        try {
            int bytesRead = 0;
            //todo: set header variables for 1 of 2, 2 of 2, etc.
            while ((bytesRead = inStream.read(buffer.array())) != -1) {
                message = new Message(componentNode.getId());

                if (bytesRead == sizePerMessage * 1024) {
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

    private void applySettings() {
        properties = componentNode.getComponentVersion().toTypedProperties(this, false);
        relativePathAndFile = properties.get(BINARYFILEREADER_RELATIVE_PATH);
        mustExist = properties.is(BINARYFILEREADER_MUST_EXIST);
        sizePerMessage = properties.getInt(BINARYFILEREADER_SIZE_PER_MESSAGE);
    }

    private byte[] trimByteArray(byte[] byteArray, int size) {
        byte[] newArray = new byte[size];
        System.arraycopy(byteArray, 0, newArray, 0, size);
        return newArray;
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
        } catch (IOException e) {
            throw new IoException("Failure in closing the reader " + e.getMessage());
        }
    }
}
