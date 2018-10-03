package org.jumpmind.metl.core.model;

public class ModelSchemaObjectRel extends AbstractObject {

    private static final long serialVersionUID = 1L;

    String modelId;
    String parentId;
    String childId;
    
    public ModelSchemaObjectRel(String modelId, String parentId, String childId) {
        this.modelId = modelId;
        this.parentId = parentId;
        this.childId = childId;
    }
    
    public String getModelId() {
        return modelId;
    }
    public void setModelId(String modelId) {
        this.modelId = modelId;
    }
    public String getParentId() {
        return parentId;
    }
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    public String getChildId() {
        return childId;
    }
    public void setChildId(String childId) {
        this.childId = childId;
    }
}
