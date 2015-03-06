package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.IOException;
import java.io.OutputStream;

import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;
import org.jumpmind.symmetric.is.core.runtime.resource.localfile.IStreamableResource;

@ComponentDefinition(typeName = TextFileWriter.TYPE, category = ComponentCategory.WRITER,
        supports = { ComponentSupports.INPUT_MESSAGE },
        resourceCategory = ResourceCategory.RESOURCE)
public class BinaryFileWriter extends AbstractComponent {

    public static final String TYPE = "Binary File Writer";

    public static final String DEFAULT_CHARSET = "UTF-8";

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Path and File")
    public final static String BINARYFILEWRITER_RELATIVE_PATH = "binaryfilewriter.relative.path";

    @SettingDefinition(type = Type.BOOLEAN, order = 20, required = true, provided = true,
            defaultValue = "false", label = "Must Exist")
    public static final String BINARYFILEWRITER_MUST_EXIST = "binaryfilewriter.must.exist";

    @SettingDefinition(type = Type.BOOLEAN, order = 30, required = true, provided = true,
            defaultValue = "false", label = "Append")
    public static final String BINARYFILEWRITER_APPEND = "binaryfilewriter.append";

    /* settings */
    String relativePathAndFile;
    boolean mustExist;
    boolean append;
    String lineTerminator;

    /* other vars */
    TypedProperties properties;
    OutputStream outStream;

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        byte[] payload = (byte[]) inputMessage.getPayload();
        if (inputMessage.getHeader().getSequenceNumber() == 1) {
            initStream();
        }
        try {
            outStream.write(payload);
        } catch (IOException e) {
            throw new IoException("Error writing to file" + e.getMessage());
        }
        if (inputMessage.getHeader().isLastMessage()) {
            close();
        }
    }

    @Override
    public void stop() {
        close();
        super.stop();
    }

    private void initStream() {
        outStream = getOutputStream((IStreamableResource) this.resource.reference());        
    }
    
    private void applySettings() {
        properties = flowStep.getComponentVersion().toTypedProperties(this, false);
        relativePathAndFile = properties.get(BINARYFILEWRITER_RELATIVE_PATH);
        mustExist = properties.is(BINARYFILEWRITER_MUST_EXIST);
        append = properties.is(BINARYFILEWRITER_APPEND);
    }


    private OutputStream getOutputStream(IStreamableResource conn) {
        conn.appendPath(relativePathAndFile, mustExist);
        return conn.getOutputStream();
    }


    private void close() {
        try {
            if (outStream != null) {
                outStream.close();
            }
        } catch (IOException e) {
            throw new IoException("Failure in closing the writer " + e.getMessage());
        }
    }
}