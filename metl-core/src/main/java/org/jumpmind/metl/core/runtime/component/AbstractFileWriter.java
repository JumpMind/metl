package org.jumpmind.metl.core.runtime.component;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;

public abstract class AbstractFileWriter extends AbstractComponentRuntime {

    public final static String SETTING_RELATIVE_PATH = "relative.path";
    public static final String SETTING_MUST_EXIST = "must.exist";
    public static final String SETTING_APPEND = "append";
    public static final String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";
    public static final String SETTING_FILENAME_PROPERTY = "filename.property";

    String relativePathAndFile;
    boolean mustExist;
    boolean append;
    boolean getFileNameFromMessage;
    String fileNameFromMessageProperty;
    
    protected void init() {
    	
        TypedProperties properties = getTypedProperties();
        relativePathAndFile = FormatUtils.replaceTokens(properties.get(SETTING_RELATIVE_PATH), context.getFlowParameters(),
                true);
        mustExist = properties.is(SETTING_MUST_EXIST);
        append = properties.is(SETTING_APPEND);
        getFileNameFromMessage = properties.is(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        fileNameFromMessageProperty = properties.get(SETTING_FILENAME_PROPERTY, fileNameFromMessageProperty);

    }
    
    protected String getFileName(Message inputMessage) {
    	
    	String fileName = null;
    	if (getFileNameFromMessage) {
    		Object objFileName = inputMessage.getHeader().get(fileNameFromMessageProperty);
			if (objFileName == null || ((String) objFileName).length() == 0) {
				throw new RuntimeException("Configuration determines that the file name should be in "
						+ "the message header but was not.  Verify the property " + 
						fileNameFromMessageProperty + " is being passed into the message header");
    		}
	    	fileName = (String) objFileName;
    	}
    	return fileName;
    }
    
    protected IDirectory initStream(String fileName) {
    	
        IDirectory streamable = (IDirectory) getResourceReference();
        streamable.delete(fileName);
        
        return streamable;
    }
}
