package org.jumpmind.metl.core.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    
}
