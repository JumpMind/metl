package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IStreamable;
import org.jumpmind.util.FormatUtils;

public class TextFileWriter extends AbstractComponentRuntime {

    public static final String TYPE = "Text File Writer";

    public static final String DEFAULT_CHARSET = "UTF-8";

    public final static String TEXTFILEWRITER_RELATIVE_PATH = "textfilewriter.relative.path";

    public static final String TEXTFILEWRITER_MUST_EXIST = "textfilewriter.must.exist";

    public static final String TEXTFILEWRITER_APPEND = "textfilewriter.append";

    public static final String TEXTFILEWRITER_TEXT_LINE_TERMINATOR = "textfilereader.text.line.terminator";

    String relativePathAndFile;
    
    boolean mustExist;
    
    boolean append;
    
    String lineTerminator;

    TypedProperties properties;
    
    BufferedWriter bufferedWriter = null;

    @Override
    protected void start() {        
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
                    bufferedWriter.write(lineTerminator);
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
        IStreamable streamable = (IStreamable) getResourceReference();
        if (!append && streamable.supportsDelete()) {
            streamable.delete(relativePathAndFile);
        }
        log(LogLevel.INFO,  String.format("Writing text file to %s", streamable.toString()));
        bufferedWriter = initializeWriter(streamable.getOutputStream(relativePathAndFile, mustExist));        
    }
    
    private void applySettings() {
        properties = getTypedProperties();
        relativePathAndFile = FormatUtils.replaceTokens(properties.get(TEXTFILEWRITER_RELATIVE_PATH), context.getFlowParametersAsString(), true);
        mustExist = properties.is(TEXTFILEWRITER_MUST_EXIST);
        append = properties.is(TEXTFILEWRITER_APPEND);
        lineTerminator = properties.get(TEXTFILEWRITER_TEXT_LINE_TERMINATOR);
    }


    private BufferedWriter initializeWriter(OutputStream stream) {
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(stream, DEFAULT_CHARSET));
        } catch (UnsupportedEncodingException e) {
            throw new IoException("Error creating buffered writer " + e.getMessage());
        }
        return bufferedWriter;
    }

    private void close() {
        IOUtils.closeQuietly(bufferedWriter);
    }

}
