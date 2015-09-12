package org.jumpmind.metl.core.model;

public enum DataType {

    BIT, BOOLEAN,
    TINYINT, SMALLINT, INTEGER, BIGINT,
    FLOAT, REAL, DOUBLE, NUMERIC, DECIMAL,
    CHAR, VARCHAR, LONGVARCHAR, NCHAR, NVARCHAR, LONGNVARCHAR,
    DATE, TIME, TIMESTAMP,
    BINARY, VARBINARY, LONGVARBINARY,
    NULL, OTHER, JAVA_OBJECT, DISTINCT, STRUCT, ARRAY,
    BLOB, CLOB, NCLOB, 
    REF, DATALINK, ROWID, SQLXML;

    public boolean isBoolean() {
        return this.equals(BIT) || this.equals(BOOLEAN);
    }

    public boolean isNumeric() {
        return this.equals(TINYINT) || this.equals(SMALLINT) || this.equals(INTEGER) || this.equals(BIGINT);
    }
    
    public boolean isTimestamp() {
        return this.equals(DATE) || this.equals(TIME) || this.equals(TIMESTAMP);
    }

    public boolean isString() {
        return this.equals(CHAR) || this.equals(VARCHAR) || this.equals(LONGVARCHAR) || this.equals(NCHAR) || 
                this.equals(NVARCHAR) || this.equals(LONGNVARCHAR) || this.equals(CLOB) || this.equals(NCLOB);
    }
    
    public boolean isBinary() {
        return this.equals(BINARY) || this.equals(VARBINARY) || this.equals(LONGVARBINARY) || this.equals(BLOB);
    }
    
}
