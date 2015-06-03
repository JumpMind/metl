package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IStreamable;

public class BinaryFileReader extends AbstractComponentRuntime {

    public static final String TYPE = "Binary File Reader";

    public final static String BINARYFILEREADER_RELATIVE_PATH = "binaryfilereader.relative.path";

    public static final String BINARYFILEREADER_MUST_EXIST = "binaryfilereader.must.exist";

    public static final String BINARYFILEREADER_SIZE_PER_MESSAGE = "binaryfilereader.size.per.message";

    String relativePathAndFile;

    boolean mustExist;
    
    int sizePerMessage;

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
