package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ModelVersionData;

public class ModelVersion extends AbstractObject<ModelVersionData> {

    private static final long serialVersionUID = 1L;

    Model model;
    
    ModelFormat modelFormat;
    
    List<ModelEntity> modelEntities;
    
    public ModelVersion(Model model, ModelFormat modelFormat, ModelVersionData data) {
        super(data);
        this.model = model;
        this.modelFormat = modelFormat;
        data.setModelId(model.getData().getId());
        //TODO: add model format classes
//        if (modelFormat != null) {
//            data.setModelFormatId(modelFormat.getData().getId());
//        }
    }

    public String getVersionName() {
        return data.getVersionName();
    }
    
    public void setVersionName(String versionName) {
        this.data.setVersionName(versionName);
    }
    
    public void setModel(Model model) {
        this.model = model;
        this.data.setModelId(model.getId());
    }
    
    public Model getModel() {
        return model;
    }
    
    public List<ModelEntity> getModelEntities() {
    	return modelEntities;
    }
    
    public void setName(String name) {
    }
    
    public String getName() {
        return this.model.getName() + " " + data.getVersionName();
    }
    
    //TODO: add model format classes
//    public void setModelFormat(ModelFormat modelFormat) {
//        this.modelFormat = modelFormat;
//        this.data.setModelFormatId(modelFormat.getId());
//    }
//    
//    public ModelFormat getModelFormat() {
//        return modelFormat;
//    }
        
}
