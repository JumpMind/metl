package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;

import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.config.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.localfile.IStreamableConnection;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

public class FileWriterComponent extends AbstractComponent {

	public static final String TYPE = "File Writer";
	private static final String DEFAULT_CHARSET = "UTF-8";

	@SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Path and File")
	public final static String FILEWRITER_RELATIVE_PATH = "filewriter.relative.path";

	@SettingDefinition(type = Type.BOOLEAN, order = 20, required = true, provided = true, defaultValue = "false", label = "Must Exist")
	public static final String FILEWRITER_MUST_EXIST = "filewriter.must.exist";

	@SettingDefinition(type = Type.BOOLEAN, order = 30, required = true, provided = true, defaultValue = "false", label = "Append")
	public static final String FILEWRITER_APPEND = "filewriter.append";

	TypedProperties properties;
	OutputStream outStream;
	BufferedWriter bufferedWriter;

	@Override
	public void start(IExecutionTracker executionTracker,
			IConnectionFactory connectionFactory) {
		super.start(executionTracker, connectionFactory);
		properties = componentNode.getComponentVersion().toTypedProperties(
				this, false);
		outStream = getOutputStream((IStreamableConnection) this.connection
				.reference());
		bufferedWriter = initializeWriter(outStream);
	}

	@Override
	public void handle(Message inputMessage, IMessageTarget messageTarget) {
		try{
			bufferedWriter.write((inputMessage.getPayload().toString()));
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
	
	protected OutputStream getOutputStream(IStreamableConnection conn) {
		conn.appendPath(this.properties.get(FILEWRITER_RELATIVE_PATH),
				this.properties.is(FILEWRITER_MUST_EXIST));
		return conn.getOutputStream();
	}

	protected BufferedWriter initializeWriter(OutputStream stream) {
		try {
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(
					outStream, DEFAULT_CHARSET));
		} catch (UnsupportedEncodingException e) {
			throw new IoException("Error setting default encoding "
					+ e.getMessage());
		}
		return bufferedWriter;
	}

	protected void close() {
		try {
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
		} catch (IOException e) {
			throw new IoException("Failure in closing the writer "
					+ e.getMessage());
		} finally {
			closeStream();
		}
	}

	protected void closeStream() {
		try {
			if (outStream != null) {
				outStream.close();
			}
		} catch (IOException e) {
			throw new IoException("Failure in closing the writer "
					+ e.getMessage());
		}
	}

}