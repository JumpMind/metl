package org.jumpmind.metl.core.model;

import org.jumpmind.db.sql.Row;

public class EntityRow extends Row {

    private static final long serialVersionUID = 1L;
    
    String entityName;

    public EntityRow(String entityName, int numberOfColumns) {
        super(numberOfColumns);
        this.entityName = entityName;
    }

    public EntityRow(String entityName, String columnName, Object value) {
        super(columnName, value);
        this.entityName = entityName;
    }
    
    public String getEntityName() {
        return entityName;
    }
    
    public void setEntityName(String entityName) {
        this.entityName = entityName;
    }

}
