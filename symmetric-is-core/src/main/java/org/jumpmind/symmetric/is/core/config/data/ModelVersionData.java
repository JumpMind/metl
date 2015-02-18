package org.jumpmind.symmetric.is.core.config.data;


public class ModelVersionData extends AbstractVersionData {

    private static final long serialVersionUID = 1L;
    
    String versionName;
    
    String modelId;
        
	public String getVersionName() {
		return versionName;
	}
	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}
	public String getModelId() {
		return modelId;
	}
	public void setModelId(String modelId) {
		this.modelId = modelId;
	}
}
