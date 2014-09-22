package org.jumpmind.symmetric.is.core.persist;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.apache.commons.beanutils.PropertyUtils;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.SqlException;

public class JdbcPersistenceManager extends AbstractPersistenceManager {

    IDatabasePlatform databasePlatform;

    public JdbcPersistenceManager(IDatabasePlatform databasePlatform) {
        this.databasePlatform = databasePlatform;
    }

    @Override
    public boolean save(Object object) {
        return save(object, null, null, camelCaseToUnderScores(object.getClass().getSimpleName()));
    }

    /**
     * Save an object to a table
     * 
     * @param object
     * @param catalogName
     * @param schemaName
     * @param tableName
     * @return true if the object was created, false if the object was updated
     */
    @Override
    public boolean save(Object object, String catalogName, String schemaName, String tableName) {
        if (update(object, catalogName, schemaName, tableName) == 0) {
            insert(object, catalogName, schemaName, tableName);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int update(Object object, String catalogName, String schemaName, String tableName) {
        return dml(DmlType.UPDATE, object, catalogName, schemaName, tableName);
    }

    @Override
    public void insert(Object object, String catalogName, String schemaName, String tableName) {
        dml(DmlType.INSERT, object, catalogName, schemaName, tableName);
    }
    
    public boolean delete(Object object) {
        return delete(object, null, null, camelCaseToUnderScores(object.getClass().getSimpleName()));
    }
    
    public boolean delete(Object object, String catalogName, String schemaName, String tableName) {
        return dml(DmlType.DELETE, object, catalogName, schemaName, tableName) > 0;
    }

    protected int dml(DmlType type, Object object, String catalogName, String schemaName,
            String tableName) {
        Table table = findTable(catalogName, schemaName, tableName);

        LinkedHashMap<String, Column> objectToTableMapping = mapObjectToTable(object, table);
        LinkedHashMap<String, Object> objectValuesByColumnName = getObjectValuesByColumnName(
                object, objectToTableMapping);

        Column[] columns = objectToTableMapping.values().toArray(
                new Column[objectToTableMapping.size()]);
        List<Column> keys = new ArrayList<Column>(1);
        for (Column column : columns) {
            if (column.isPrimaryKey()) {
                keys.add(column);
            }
        }

        boolean[] nullKeyValues = new boolean[keys.size()];
        int i = 0;
        for (Column column : keys) {
            nullKeyValues[i++] = objectValuesByColumnName.get(column.getName()) == null;
        }

        DmlStatement statement = databasePlatform.createDmlStatement(type, table.getCatalog(),
                table.getSchema(), table.getName(), keys.toArray(new Column[keys.size()]), columns,
                nullKeyValues, null);
        String sql = statement.getSql();
        Object[] values = statement.getValueArray(objectValuesByColumnName);
        int[] types = statement.getTypes();

        return databasePlatform.getSqlTemplate().update(sql, values, types);

    }

    private Table findTable(String catalogName, String schemaName, String tableName) {
        Table table = databasePlatform.getTableFromCache(catalogName, schemaName, tableName, false);
        if (table == null) {
            throw new SqlException("Could not find table "
                    + Table.getFullyQualifiedTableName(catalogName, schemaName, tableName));
        } else {
            return table;
        }
    }

    protected LinkedHashMap<String, Object> getObjectValuesByColumnName(Object object,
            LinkedHashMap<String, Column> objectToTableMapping) {
        try {
            LinkedHashMap<String, Object> objectValuesByColumnName = new LinkedHashMap<String, Object>();
            Set<String> propertyNames = objectToTableMapping.keySet();
            for (String propertyName : propertyNames) {
                objectValuesByColumnName.put(objectToTableMapping.get(propertyName).getName(),
                        PropertyUtils.getProperty(object, propertyName));
            }
            return objectValuesByColumnName;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    protected LinkedHashMap<String, Column> mapObjectToTable(Object object, Table table) {
        LinkedHashMap<String, Column> columnNames = new LinkedHashMap<String, Column>();
        PropertyDescriptor[] pds = PropertyUtils.getPropertyDescriptors(object);
        for (int i = 0; i < pds.length; i++) {
            String propName = pds[i].getName();
            Column column = table.getColumnWithName(camelCaseToUnderScores(propName));
            if (column != null) {
                columnNames.put(propName, column);
            }
        }
        return columnNames;
    }

    protected String camelCaseToUnderScores(String camelCaseName) {
        StringBuilder underscoredName = new StringBuilder();
        for (int p = 0; p < camelCaseName.length(); p++) {
            char c = camelCaseName.charAt(p);
            if (p > 0 && Character.isUpperCase(c)) {
                underscoredName.append("_");
            }
            underscoredName.append(Character.toLowerCase(c));

        }
        return underscoredName.toString();
    }

    public void setDatabasePlatform(IDatabasePlatform databasePlatform) {
        this.databasePlatform = databasePlatform;
    }

    public IDatabasePlatform getDatabasePlatform() {
        return databasePlatform;
    }
}
