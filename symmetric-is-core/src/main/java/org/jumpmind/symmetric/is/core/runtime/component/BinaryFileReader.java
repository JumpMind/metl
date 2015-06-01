package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IStreamable;

@ComponentDefinition(typeName = BinaryFileReader.TYPE, category = ComponentCategory.READER, iconImage="binaryfilereader.png",
        outgoingMessage=MessageType.BINARY,
        resourceCategory = ResourceCategory.STREAMABLE)
public class BinaryFileReader extends AbstractComponentRuntime {

    public static final String TYPE = "Binary File Reader";

    @SettingDefinition(order = 10, required = true, type = Type.TEXT, label = "Path and File")
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
    protected void start() {
        applySettings();
    }

    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {
        int numberMessages = 0;
        ByteBuffer buffer = ByteBuffer.allocate(sizePerMessage * 1024);
        open();
        try {
            int bytesRead = 0;
            while ((bytesRead = inStream.read(buffer.array())) != -1) {
                if (bytesRead == sizePerMessage * 1024) {
                    initAndSendMessage(buffer.array().clone(), messageTarget, numberMessages, false);
                } else {
                    initAndSendMessage(trimByteArray(buffer.array().clone(), bytesRead), messageTarget, numberMessages, true);
                }
                buffer.clear();
            }
        } catch (IOException e) {
            throw new IoException("Error reading from file " + e.getMessage());
        } finally {
            close();
        }
    }

    private void initAndSendMessage(byte[] payload, IMessageTarget messageTarget,
            int numberMessages, boolean lastMessage) {
        numberMessages++;
        Message message = new Message(getFlowStepId());
        message.getHeader().setSequenceNumber(numberMessages);
        message.getHeader().setLastMessage(lastMessage);
        message.setPayload(payload);
        messageTarget.put(message);
    }
    
    private void applySettings() {
        properties = getComponent().toTypedProperties(getSettingDefinitions(false));
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
        IStreamable resource = (IStreamable) getResourceReference();
        inStream = resource.getInputStream(relativePathAndFile, mustExist);
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
