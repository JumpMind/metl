package org.jumpmind.metl.core.model;

import java.io.Serializable;
import java.util.Map;

public class EntityRow implements Serializable {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    Map<String, Object> data;

    public EntityRow(String entityName, Map<String, Object> row) {
        this.name = entityName;
        this.data = row;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String entityName) {
        this.name = entityName;
    }
    
    public Map<String, Object> getData() {
        return data;
    }
    
    public void setData(Map<String, Object> row) {
        this.data = row;
    }

}
