package org.jumpmind.metl.core.runtime.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class BinaryFileReader extends AbstractFileReader {

    public static final String TYPE = "Binary File Reader";
    public static final String SETTING_SIZE_PER_MESSAGE = "size.per.message";

    int sizePerMessage = 100;
    @Override
    public void start() {
    	init();
        Component component = getComponent();
        sizePerMessage = component.getInt(SETTING_SIZE_PER_MESSAGE, sizePerMessage);
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
			List<String> files = getFilesToRead(inputMessage);
    		processFiles(files, inputMessage, callback, unitOfWorkBoundaryReached);
    	}
    }

    private void processFiles(List<String> files, Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {

        filesRead.addAll(files);

        for (String file : files) {
            Map<String, Serializable> headers = new HashMap<>(1);
            headers.put("source.file.path", file);
            InputStream inStream = null;
            try {
                info("Reading file: %s", file);
                String filePath = resolveParamsAndHeaders(file, inputMessage);
                inStream = directory.getInputStream(filePath, mustExist);
                //TODO: if the file is bigger than the allowable message size, this doesn't work
                if (inStream != null) {
                    byte[] payload = IOUtils.toByteArray(inStream);
                    callback.sendBinaryMessage(headers, payload);
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);               
                } else {
                    info("File %s didn't exist, but Must Exist setting was false.  Continuing",file);
                }
            } catch (IOException e) {
                throw new IoException("Error reading from file " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(inStream);
            }
            
            if (controlMessageOnEof) {
            	callback.sendControlMessage(headers);
            }
        }
    }
}
