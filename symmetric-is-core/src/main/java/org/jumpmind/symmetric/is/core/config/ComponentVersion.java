package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ComponentVersionData;
import org.jumpmind.symmetric.is.core.config.data.ComponentVersionSettingData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class ComponentVersion extends
		AbstractObjectWithSettings<ComponentVersionData> {

	private static final long serialVersionUID = 1L;

	Connection connection;

	Component component;

	ModelVersion inputModelVersion;

	ModelVersion outputModelVersion;

	public ComponentVersion(Component component, Connection connection,
			ModelVersion inputModelVersion, ModelVersion outputModelVersion,
			ComponentVersionData data, SettingData... settings) {
		super(data, settings);
		this.component = component;
		this.connection = connection;
		this.inputModelVersion = inputModelVersion;
		this.outputModelVersion = outputModelVersion;
		data.setComponentId(component.getData().getId());
		if (connection != null) {
			data.setConnectionId(connection.getData().getId());
		}
	}

	public String getName() {
		return data.getName();
	}

	public void setName(String name) {
		this.data.setName(name);
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
		this.data.setConnectionId(connection.getId());
	}

	public Connection getConnection() {
		return connection;
	}

	public Component getComponent() {
		return component;
	}

	@Override
	protected SettingData createSettingData() {
		return new ComponentVersionSettingData(data.getId());
	}

}
