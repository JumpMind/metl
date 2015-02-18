package org.jumpmind.symmetric.is.core.config;


public class ComponentVersion extends
		AbstractObjectWithSettings {

	private static final long serialVersionUID = 1L;

	Connection connection;

	Component component;

	ModelVersion inputModelVersion;

	ModelVersion outputModelVersion;
	
    String inputModelVersiondId;
    
    String outputModelVersionId;
    
    String connectionId;
    
    String componentId;
    
    String versionName;
	
    public ComponentVersion() {
    }
    
	public ComponentVersion(Component component, Connection connection,
			ModelVersion inputModelVersion, ModelVersion outputModelVersion,
			Setting... settings) {
	    super(settings);
		this.component = component;
		this.connection = connection;
		this.inputModelVersion = inputModelVersion;
		this.outputModelVersion = outputModelVersion;
		this.componentId = component.getId();
		if (connection != null) {
			connectionId = connection.getId();
		}
	}
	
	public void setInputModelVersion(ModelVersion inputModelVersion) {
        this.inputModelVersion = inputModelVersion;
        if (inputModelVersion != null) {
            this.inputModelVersiondId = inputModelVersion.getId();
        } else {
            this.inputModelVersiondId = null;
        }
    }
	
	public void setOutputModelVersion(ModelVersion outputModelVersion) {
        this.outputModelVersion = outputModelVersion;
        if (outputModelVersion != null) {
            outputModelVersionId = outputModelVersion.getId();
        } else {
            outputModelVersionId = null;
        }
    }
	
	public void setComponent(Component component) {
        this.component = component;
        if (component != null) {
            this.componentId = component.getId();
        } else {
            this.componentId = null;
        }
    }

	public void setConnection(Connection connection) {
		this.connection = connection;
		if (connection != null) {
		connectionId = connection.getId();
		} else {
		    connectionId = null;
		}
	}

	public Connection getConnection() {
		return connection;
	}

	public Component getComponent() {
		return component;
	}

    public String getInputModelVersiondId() {
        return inputModelVersiondId;
    }

    public void setInputModelVersiondId(String inputModelVersiondId) {
        this.inputModelVersiondId = inputModelVersiondId;
    }

    public String getOutputModelVersionId() {
        return outputModelVersionId;
    }

    public void setOutputModelVersionId(String outputModelVersionId) {
        this.outputModelVersionId = outputModelVersionId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }
    
    public String getVersionName() {
        return versionName;
    }
    
    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }
    
    public void setName(String name) {
    }
    
    public String getName() {
        return this.component.getName();
    }
    
    public ModelVersion getInputModelVersion() {
    	return inputModelVersion;
    }
    
    public ModelVersion getOutputModelVersion() {
    	return outputModelVersion;
    }
    
	@Override
	protected Setting createSettingData() {
		return new ComponentVersionSetting(id);
	}

}
