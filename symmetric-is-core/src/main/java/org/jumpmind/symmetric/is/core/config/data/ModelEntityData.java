package org.jumpmind.symmetric.is.core.config.data;

public class ModelEntityData extends AbstractVersionData  {

    private static final long serialVersionUID = 1L;

    String modelVersionId;
    
    String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModelVersionId() {
		return modelVersionId;
	}

	public void setModelVersionId(String modelVersionId) {
		this.modelVersionId = modelVersionId;
	}
  
}
