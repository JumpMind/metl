package org.jumpmind.metl.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;

public class EntityTable implements Serializable {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
    
    public EntityTable(String entityName) {
        this.name = entityName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Map<String, String>> getRows() {
        return rows;
    }

    public void setRows(List<Map<String, String>> rows) {
        this.rows = rows;
    }
    
    public EntityData toEntityData(Model model) {
        
        EntityData entityData = new EntityData();
        ModelEntity entity = model.getEntityByName(name);
        for (Map<String, String> row:rows) {
            for (Map.Entry<String, String> entry : row.entrySet()) {
                entityData.put(entity.getModelAttributeByName(entry.getKey()).getId(),entry.getValue());
            }
        }
        return entityData;
    }   
}
