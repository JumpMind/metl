package org.jumpmind.metl.core.runtime.component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IDirectory;

public class BinaryFileWriter extends AbstractFileWriter {

    public static final String TYPE = "Binary File Writer";
    OutputStream fos = null;
    
	@Override
	protected void start() {
		init();
	}
	
	@Override
	public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
		
		IDirectory streamable;
		if (inputMessage instanceof BinaryMessage) {
		    BinaryMessage message = (BinaryMessage)inputMessage;
			String fileName = getFileName(inputMessage);
			streamable = initStream(fileName);
			initFos(streamable, fileName);
			try {
				fos.write(message.getPayload());
			} catch(IOException e) {
				throw new IoException(e);
			}
		} 
		
        if ((inputMessage instanceof ControlMessage ||
        		unitOfWorkBoundaryReached) && callback != null) {
        	IOUtils.closeQuietly(fos);
        	ArrayList<String> results = new ArrayList<>();
        	results.add("{\"status\":\"success\"}");
            callback.sendTextMessage(null, results);			
		}
	}

	@Override
	public boolean supportsStartupMessages() {
		return false;
	}
	
	private void initFos(IDirectory streamable, String fileName) {
		fos = streamable.getOutputStream(fileName, mustExist);
	}
}
