package org.jumpmind.metl.core.runtime.component;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.jmx.AppenderDynamicMBean;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IStreamable;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.util.FormatUtils;
import org.springframework.util.FileCopyUtils;

public class FileUtil extends AbstractComponentRuntime {
	public static final String ACTION_COPY = "Copy";
	public static final String ACTION_RENAME = "Rename";
	public static final String ACTION_MOVE = "Move";
	public static final String ACTION_DELETE = "Delete";
	public static final String ACTION_TOUCH = "Touch";
	
	public final static String SETTING_ACTION = "action";

	public final static String SETTING_RELATIVE_PATH = "relative.path";

	public final static String SETTING_GET_FILE_FROM_MESSAGE = "get.file.name.from.message";

	public static final String SETTING_MUST_EXIST = "must.exist";

	public static final String SETTING_TARGET_DIRECTORY = "target.directory";

	public static final String SETTING_NEW_NAME = "new.name";

	public static final String SETTING_APPEND_TO_NAME = "append.to.name";
	
	String action = ACTION_COPY;
	
	boolean getFileNameFromMessage = false;
	
	String relativePathAndFile;

    boolean mustExist = false;
    
	String targetDirectory;
	
	String newName;
	
	String appendToName;
	
	@Override
	public boolean supportsStartupMessages() {
		return true;
	}

	@Override
	protected void start() {
		Component component = getComponent();
        getFileNameFromMessage = component.getBoolean(SETTING_GET_FILE_FROM_MESSAGE, getFileNameFromMessage);
        relativePathAndFile = component.get(SETTING_RELATIVE_PATH, relativePathAndFile);
        mustExist = component.getBoolean(SETTING_MUST_EXIST, mustExist);
        targetDirectory = component.get(SETTING_TARGET_DIRECTORY, targetDirectory);
        newName = component.get(SETTING_NEW_NAME, newName);
        appendToName = component.get(SETTING_APPEND_TO_NAME);
    }
	
	@Override
	public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {
		List<String> files = getFilesToRead(inputMessage);
		for (String fileName : files) {
			try {
			if (action.equals(ACTION_COPY)) {
				copyFile(fileName);
			}
			} catch (Exception e) {
				throw new IoException("Error processing file " + e.getMessage());
			}
		}
	}

	protected void copyFile(String fileName) throws Exception {
		
		 IStreamable resource = (IStreamable) getResourceReference();
		 File sourceFile = new File(fileName);
		 if (mustExist && !sourceFile.exists()) {
			 throw new FileNotFoundException("Unable to locate file " + fileName);
		 }
		 else {
			 long originalSize = sourceFile.length();
			 
			 String tokenResolvedName = FormatUtils.replaceTokens(newName, getComponentContext().getFlowParametersAsString(), true);
			 String tokenResolvedAppendToName = FormatUtils.replaceTokens(appendToName, getComponentContext().getFlowParametersAsString(), true);
			 
			 String targetFileWithouPart = getTargetFileName(tokenResolvedName, sourceFile, tokenResolvedAppendToName);
			 String targetFileName = targetFileWithouPart + ".part";
			 
			 File targetFile = new File(targetDirectory, targetFileName);
			 targetFile.getParentFile().mkdirs();
			 int copiedSize = FileCopyUtils.copy(sourceFile, targetFile);
			 if (originalSize == copiedSize) {
				 targetFile.renameTo(new File(targetDirectory, targetFileWithouPart));
			 }
		 }
	}
	
	protected String getTargetFileName(String tokenResolvedName, File sourceFile, String tokenResolvedAppendToName) {
		String fileName = (tokenResolvedName != null ? tokenResolvedName : sourceFile.getName());
		 if (tokenResolvedAppendToName != null && tokenResolvedAppendToName.length() > 0) {
			 String[] parts = fileName.split("\\.");
			 if (parts.length > 1) {
				 StringBuffer sb = new StringBuffer()
						 .append(parts[0])
						 .append(tokenResolvedAppendToName)
						 .append(".")
						 .append(parts[1]);
				 fileName = sb.toString();
			 }
		 }
		 return fileName;
	}
	private List<String> getFilesToRead(Message inputMessage) {
        ArrayList<String> files = new ArrayList<String>();
        if (getFileNameFromMessage) {
            files = inputMessage.getPayload();
        } else {
            files.add(relativePathAndFile);
        }
        return files;
    }

}
