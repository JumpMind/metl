package org.jumpmind.metl.core.runtime.component;

import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
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
		if (!(inputMessage instanceof ControlMessage)) {			
			String fileName = getFileName(inputMessage);
			streamable = initStream(fileName);
			initFos(streamable, fileName);
			try {
				fos.write(inputMessage.getPayload());
			} catch(IOException e) {
				throw new IoException(e);
			}
		} 
		
        if ((inputMessage instanceof ControlMessage ||
        		unitOfWorkBoundaryReached) && callback != null) {
        	IOUtils.closeQuietly(fos);
            callback.sendMessage(null, "{\"status\":\"success\"}");			
		}
	}

	@Override
	public boolean supportsStartupMessages() {
		// TODO Auto-generated method stub
		return false;
	}
	
	private void initFos(IDirectory streamable, String fileName) {
		fos = streamable.getOutputStream(fileName, mustExist);
	}
}
