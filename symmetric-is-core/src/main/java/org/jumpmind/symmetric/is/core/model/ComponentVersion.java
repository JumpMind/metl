package org.jumpmind.symmetric.is.core.model;


public class ComponentVersion extends
		AbstractObjectWithSettings {

	private static final long serialVersionUID = 1L;

	Connection connection;

	Component component;

	ModelVersion inputModelVersion;

	ModelVersion outputModelVersion;
	
    String versionName;
	
    public ComponentVersion() {
    }
    
    public ComponentVersion(String id) {
    	this.id = id;
    }
    
	public ComponentVersion(Component component, Connection connection,
			ModelVersion inputModelVersion, ModelVersion outputModelVersion,
			Setting... settings) {
	    super(settings);
		this.component = component;
		this.connection = connection;
		this.inputModelVersion = inputModelVersion;
		this.outputModelVersion = outputModelVersion;
	}
	
	public void setInputModelVersion(ModelVersion inputModelVersion) {
        this.inputModelVersion = inputModelVersion;
    }
	
	public void setOutputModelVersion(ModelVersion outputModelVersion) {
        this.outputModelVersion = outputModelVersion;
    }
	
	public void setComponent(Component component) {
        this.component = component;
    }

	public void setConnection(Connection connection) {
		this.connection = connection;
	}

	public Connection getConnection() {
		return connection;
	}

	public Component getComponent() {
		return component;
	}

    public String getInputModelVersiondId() {
        return inputModelVersion != null ? inputModelVersion.getId() : null;
    }

    public void setInputModelVersiondId(String inputModelVersiondId) {
        if (inputModelVersiondId != null) {
            this.inputModelVersion = new ModelVersion(inputModelVersiondId);
        } else {
            this.inputModelVersion = null;
        }
    }

    public String getOutputModelVersionId() {
        return inputModelVersion != null ? inputModelVersion.getId() : null;
    }

    public void setOutputModelVersionId(String outputModelVersionId) {
        if (outputModelVersionId != null) {
            this.outputModelVersion = new ModelVersion(outputModelVersionId);
        } else {
            this.outputModelVersion = null;
        }
    }

    public String getConnectionId() {
        return connection != null ? connection.getId() : null;
    }

    public void setConnectionId(String connectionId) {
        if (connectionId != null) {
            this.connection = new Connection(connectionId);
        } else {
            this.connection = null;
        }
    }

    public void setComponentId(String componentId) {
        if (componentId != null) {
            this.component = new Component(componentId);
        } else {
            this.component = null;
        }
    }

    public String getComponentId() {
        return component != null ? component.getId() : null;
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
