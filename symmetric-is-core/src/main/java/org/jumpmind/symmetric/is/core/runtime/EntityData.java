package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class EntityData extends LinkedCaseInsensitiveMap<Object> {

    private static final long serialVersionUID = 1L;
    
    /*
     * TODO: This should probably be the entity id.  Leaving as entity name for now for simplicities sake
     */
    String entityName;
    
    public EntityData() {
    }


    public EntityData(String entityName) {
        this.entityName = entityName;
    }


    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }
    
    public String getEntityName() {
        return entityName;
    }
    
}
