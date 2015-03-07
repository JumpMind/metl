package org.jumpmind.symmetric.is.core.model;


public class ComponentVersion extends
		AbstractObjectWithSettings {

	private static final long serialVersionUID = 1L;

	Resource resource;

	Component component;

	ModelVersion inputModelVersion;

	ModelVersion outputModelVersion;
	
    String versionName;
	
    public ComponentVersion() {
    }
    
    public ComponentVersion(String id) {
    	this.id = id;
    }

    public ComponentVersion(Component component) {
        this(component, null, null, null);
    }
        
	public ComponentVersion(Component component, Resource resource,
			ModelVersion inputModelVersion, ModelVersion outputModelVersion,
			Setting... settings) {
	    super(settings);
		this.component = component;
		this.resource = resource;
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

	public void setResource(Resource resource) {
		this.resource = resource;
	}

	public Resource getResource() {
		return resource;
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

    public String getResourceId() {
        return resource != null ? resource.getId() : null;
    }

    public void setResourceId(String resourceId) {
        if (resourceId != null) {
            this.resource = new Resource(resourceId);
        } else {
            this.resource = null;
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
