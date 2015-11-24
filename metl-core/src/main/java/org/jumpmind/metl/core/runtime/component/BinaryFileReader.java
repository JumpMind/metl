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
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.util.FormatUtils;

public class BinaryFileReader extends AbstractFileReader {

    public static final String TYPE = "Binary File Reader";
    public static final String SETTING_SIZE_PER_MESSAGE = "size.per.message";

    int sizePerMessage = 100;
    @Override
    protected void start() {
    	init();
        Component component = getComponent();
        sizePerMessage = component.getInt(SETTING_SIZE_PER_MESSAGE, sizePerMessage);
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
    	
		if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
				|| (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage))) {
			List<String> files = getFilesToRead(inputMessage);
    		processFiles(files, callback, unitOfWorkBoundaryReached);
    	}
    }

    private void processFiles(List<String> files, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
    	//parameter is in MB
        byte[] payload = new byte[sizePerMessage*1024*1024];
        int bytesRead;
        int totalBytesRead=0;

        filesRead.addAll(files);

        for (String file : files) {
            Map<String, Serializable> headers = new HashMap<>(1);
            headers.put("source.file.path", file);
            InputStream inStream = null;
            try {
                info("Reading file: %s", file);
                IDirectory resource = (IDirectory) getResourceReference();
                String filePath = FormatUtils.replaceTokens(file, context.getFlowParametersAsString(), true);
                inStream = resource.getInputStream(filePath, mustExist);
                while ((bytesRead = inStream.read(payload)) != -1) {
                	totalBytesRead += bytesRead;
                	callback.sendMessage(headers, payload);
                	payload = new byte[sizePerMessage*1024*1024];
                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                }
                
            } catch (IOException e) {
                throw new IoException("Error reading from file " + e.getMessage());
            } finally {
                IOUtils.closeQuietly(inStream);
            }
        }
    }
}
