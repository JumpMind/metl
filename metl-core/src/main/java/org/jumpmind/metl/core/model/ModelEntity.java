package org.jumpmind.metl.core.model;

import java.util.ArrayList;
import java.util.List;

public class ModelEntity extends AbstractObject {

    private static final long serialVersionUID = 1L;

    List<ModelAttribute> modelAttributes;

    String modelId;

    String name;

    public ModelEntity() {
        modelAttributes = new ArrayList<ModelAttribute>();
    }

    public ModelEntity(String Id, String name) {
        this();
        this.id = Id;
        this.name = name;
    }

    public List<ModelAttribute> getModelAttributes() {
        return modelAttributes;
    }

    public void setModelAttributes(List<ModelAttribute> modelAttributes) {
        this.modelAttributes = modelAttributes;
    }

    public void addModelAttribute(ModelAttribute modelAttribute) {
        this.modelAttributes.add(modelAttribute);
    }

    public void removeModelAttribute(ModelAttribute modelAttribute) {
        this.modelAttributes.remove(modelAttribute);
    }

    public ModelAttribute getModelAttributeByName(String name) {
        for (ModelAttribute modelAttribute : modelAttributes) {
            if (modelAttribute.getName().equalsIgnoreCase(name)) {
                return modelAttribute;
            }
        }
        return null;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelVersionId) {
        this.modelId = modelVersionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public AbstractObject copy() {
        ModelEntity entity = (ModelEntity) super.copy();
        entity.setModelAttributes(new ArrayList<ModelAttribute>());
        for (ModelAttribute modelAttribute : modelAttributes) {
            modelAttribute = (ModelAttribute) modelAttribute.copy();
            modelAttribute.setEntityId(entity.getId());
            entity.getModelAttributes().add(modelAttribute);
        }

        return entity;
    }

}
