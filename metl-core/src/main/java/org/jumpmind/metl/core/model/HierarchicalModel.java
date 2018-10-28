package org.jumpmind.metl.core.model;

import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.runtime.EntityData;

public class HierarchicalModel extends AbstractModel implements IModel {

    private static final long serialVersionUID = 1L;

    public static final String TYPE = "HIERARCHICAL";

    ModelSchemaObject rootObject;
    
    public HierarchicalModel(String id) {
        super(id);
    }
    
    public Row toRow(EntityData data) {
        //TODO:
        return null;
    }

    public ModelSchemaObject getRootObject() {
        return rootObject;
    }

    public void setRootObject(ModelSchemaObject rootObject) {
        this.rootObject = rootObject;
    }
    
    public ModelSchemaObject getObjectById(String objectId) {
        return getObjectById(rootObject, objectId);
    }
    
    private ModelSchemaObject getObjectById(ModelSchemaObject object, String objectId) {
        ModelSchemaObject objectToReturn = null;
        for (ModelSchemaObject childObject : object.getChildObjects()) {
            if (childObject.getId().equals(objectId)) {
                objectToReturn = childObject;
            }
        }
        if (objectToReturn == null) {
            for (ModelSchemaObject childObject : object.getChildObjects()) {
                getObjectById(childObject, objectId);
            }
        }
        return objectToReturn;
    }

    @Override
    public String getType() {
        return TYPE;
    }

}
