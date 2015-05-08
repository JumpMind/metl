package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IStreamableResource;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;

@ComponentDefinition(typeName = TextFileWriter.TYPE, category = ComponentCategory.WRITER, iconImage="textfilewriter.png",
        inputMessage=MessageType.TEXT,
        resourceCategory = ResourceCategory.STREAMABLE)
public class TextFileWriter extends AbstractComponentRuntime {

    public static final String TYPE = "Text File Writer";

    public static final String DEFAULT_CHARSET = "UTF-8";

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Path and File")
    public final static String TEXTFILEWRITER_RELATIVE_PATH = "textfilewriter.relative.path";

    @SettingDefinition(type = Type.BOOLEAN, order = 30, required = true, 
            defaultValue = "false", label = "Must Exist")
    public static final String TEXTFILEWRITER_MUST_EXIST = "textfilewriter.must.exist";

    @SettingDefinition(type = Type.BOOLEAN, order = 40, required = true, 
            defaultValue = "false", label = "Append")
    public static final String TEXTFILEWRITER_APPEND = "textfilewriter.append";

    @SettingDefinition(type = Type.INTEGER, order = 50, label = "Line Terminator")
    public static final String TEXTFILEWRITER_TEXT_LINE_TERMINATOR = "textfilereader.text.line.terminator";

    /* settings */
    
    String relativePathAndFile;
    boolean mustExist;
    boolean append;
    String lineTerminator;

    /* other vars */
    
    TypedProperties properties;
    OutputStream outStream;
    BufferedWriter bufferedWriter = null;

    @Override
    public void start() {
        
        applySettings();
    }

    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        
        if (getResourceRuntime() == null) {
            throw new IllegalStateException("The target resource has not been configured.  Please choose a resource.");
        }
        
        if (inputMessage.getHeader().getSequenceNumber() == 1) {
            initStreamAndWriter();
        }
        
        ArrayList<String> recs = inputMessage.getPayload();
        try {
            for (String rec : recs) {
                bufferedWriter.write(rec);
                if (StringUtils.isNotBlank(lineTerminator)) {
                    bufferedWriter.write(TEXTFILEWRITER_TEXT_LINE_TERMINATOR);
                } else {
                    bufferedWriter.newLine();
                }
            }
            bufferedWriter.flush();
        } catch (IOException e) {
            throw new IoException("Error writing to file " + e.getMessage());
        }
    }    

    @Override
    public void stop() {
        close();
        super.stop();
    }

    private void initStreamAndWriter() {
        outStream = getOutputStream((IStreamableResource) getResourceReference());
        bufferedWriter = initializeWriter(outStream);        
    }
    
    private void applySettings() {
        properties = getComponent().toTypedProperties(getSettingDefinitions(false));
        relativePathAndFile = properties.get(TEXTFILEWRITER_RELATIVE_PATH);
        mustExist = properties.is(TEXTFILEWRITER_MUST_EXIST);
        append = properties.is(TEXTFILEWRITER_APPEND);
        lineTerminator = properties.get(TEXTFILEWRITER_TEXT_LINE_TERMINATOR);
    }

    private OutputStream getOutputStream(IStreamableResource conn) {
        log(LogLevel.INFO,  String.format("Writing text file to %s", conn.toString()));
        return conn.getOutputStream(relativePathAndFile, mustExist);
    }

    private BufferedWriter initializeWriter(OutputStream stream) {
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(outStream, DEFAULT_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new IoException("Error creating buffered writer " + e.getMessage());
        }
        return bufferedWriter;
    }

    private void close() {
        try {
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        } catch (IOException e) {
            throw new IoException("Failure in closing the writer " + e.getMessage());
        } finally {
            closeStream();
        }
    }

    private void closeStream() {
        try {
            if (outStream != null) {
                outStream.close();
            }
        } catch (IOException e) {
            throw new IoException("Failure in closing the writer " + e.getMessage());
        }
    }
}
