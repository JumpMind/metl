package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ModelData;

public class Model extends AbstractObject<ModelData> {

    private static final long serialVersionUID = 1L;
    List<ModelVersion> modelVersions;
    
    public Model() {
        super(new ModelData());
    }

    public Model(ModelData data) {
        super(data);
        this.modelVersions = new ArrayList<ModelVersion>();
    }
    
    public String getName() {
    	return data.getName();
    }
    
    public String getType() {
        return data.getType();
    }

    public boolean isShared() {
        return data.isShared();
    }
    
    public List<ModelVersion> getModelVersions() {
        return modelVersions;
    }
    
    public void setModelVersions(List<ModelVersion> modelVersions) {
        this.modelVersions = modelVersions;
    }
        
}
