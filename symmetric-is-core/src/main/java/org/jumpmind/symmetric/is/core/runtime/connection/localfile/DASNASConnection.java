package org.jumpmind.symmetric.is.core.runtime.connection.localfile;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.config.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.AbstractRuntimeObject;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnection;

public class DASNASConnection extends AbstractRuntimeObject implements
		IConnection {

	public static final String TYPE = "DAS/NAS";

	@SettingDefinition(order = 0, required = true, type = Type.STRING, label = "Path")
	public final static String DASNAS_PATH = "dasnas.path";

	@SettingDefinition(type = Type.BOOLEAN, order = 20, required = true, provided = true, defaultValue = "false", label = "Must Exist")
	public static final String DASNAS_MUST_EXIST = "dasnas.must.exist";

	IStreamableConnection streamableConnection;

	@Override
	public void start(Connection connection) {
		TypedProperties properties = connection.toTypedProperties(this, false);
		streamableConnection = new FileStreamableConnection(connection,
				properties.getProperty(DASNAS_PATH), 
				properties.is(DASNAS_MUST_EXIST));
	}

	@Override
	public void stop() {
		streamableConnection.close();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T reference() {
		// TODO Auto-generated method stub
		return (T) streamableConnection;
	}

}
