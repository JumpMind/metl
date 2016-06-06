package org.jumpmind.metl.core.persist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.Row;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ImportExportService extends AbstractService implements IImportExportService {
    
    
    final String[][] MODEL_SQL = {
            {"_MODEL","SELECT * FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND ID='%3$s' ORDER BY ID"},
            {"_MODEL_ENTITY","SELECT * FROM %1$s_MODEL_ENTITY WHERE MODEL_ID='%3$s'"},
            {"_MODEL_ATTRIBUTE","SELECT * FROM %1$s_MODEL_ATTRIBUTE WHERE ENTITY_ID IN "
            + "(SELECT ID FROM %1$s_MODEL_ENTITY WHERE MODEL_ID IN "
            + "(SELECT ID FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND ID='%3$s')) ORDER BY ID"}
    };    
    final String MODEL_PK = "ID";
    
    final String[][] RESOURCE_SQL = {
            {"_RESOURCE","SELECT * FROM %1$s_RESOURCE WHERE PROJECT_VERSION_ID = '%2$s' AND ID='%3$s' ORDER BY ID"},
            {"_RESOURCE_SETTING","SELECT * FROM %1$s_RESOURCE_SETTING WHERE RESOURCE_ID='%3$s' ORDER BY NAME"}
    };
    final String RESOURCE_PK = "ID";
    
    final String[][] FLOW_SQL = {
            {"_COMPONENT","SELECT * FROM %1$s_COMPONENT WHERE PROJECT_VERSION_ID='%2$s' AND ID IN "
                    + "(SELECT DISTINCT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s') "},
            {"_COMPONENT_SETTING","SELECT * FROM %1$s_COMPONENT_SETTING WHERE COMPONENT_ID IN "
                    + "(SELECT DISTINCT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s')"},
            {"_COMPONENT_ENTITY_SETTING","SELECT * FROM %1$s_COMPONENT_ENTITY_SETTING WHERE COMPONENT_ID IN "
                    + "(SELECT DISTINCT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s')"},
            {"_COMPONENT_ATTRIBUTE_SETTING","SELECT * FROM %1$s_COMPONENT_ATTRIBUTE_SETTING WHERE COMPONENT_ID IN "
                    + "(SELECT DISTINCT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s')"},
            {"_FLOW","SELECT * FROM %1$s_FLOW WHERE PROJECT_VERSION_ID='%2$s' AND ID='%3$s' ORDER BY ID"},
            {"_FLOW_PARAMETER","SELECT * FROM %1$s_FLOW_PARAMETER WHERE FLOW_ID='%3$s'"},
            {"_FLOW_STEP","SELECT * FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s'"},
            {"_FLOW_STEP_LINK","SELECT * FROM %1$s_FLOW_STEP_LINK WHERE SOURCE_STEP_ID IN "
                    + "(SELECT DISTINCT ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s')"}            
    };
    final String FLOW_PK = "ID";
    
    private IDatabasePlatform databasePlatform;
    private String tablePrefix;
    private String[] columnsToExclude;
    private enum ExportType {MODEL, RESOURCE, FLOW};

    public ImportExportService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {

        super(persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
        this.tablePrefix = tablePrefix;
        setColumnsToExclude();
    }

    private void setColumnsToExclude() {
        columnsToExclude = new String[4];
        columnsToExclude[0] = "CREATE_TIME";
        columnsToExclude[1] = "LAST_UPDATE_TIME";
        columnsToExclude[2] = "CREATE_BY";
        columnsToExclude[3] = "LAST_UPDATE_BY";          
    }
    
    @Override
    public String exportModel(String projectVersionId, String modelId) {

        return exportConfig(ExportType.MODEL, projectVersionId, modelId);
    }
    
    @Override
    public String exportResource(String projectVersionId, String resourceId) {
        
        return exportConfig(ExportType. RESOURCE, projectVersionId, resourceId);
    }
    
    @Override
    public String exportFlow(String projectVersionId, String flowId) {
        
        return exportConfig(ExportType.FLOW, projectVersionId, flowId);
    }
    
    private String exportConfig(ExportType exportType, String projectVersionId, String objectId) {
        
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        String outData;
        ConfigData exportData=null;
        
        if (exportType.equals(ExportType.MODEL)) {
            exportData = getModelConfigData(projectVersionId, objectId);
        } else if (exportType.equals(ExportType.RESOURCE)) {
            exportData = getResourceConfigData(projectVersionId, objectId);
        } else if (exportType.equals(ExportType.FLOW)) {
            exportData = getFlowConfigData(projectVersionId, objectId);
        }
                
        try {
            outData = mapper.writeValueAsString(exportData);
        } catch (JsonProcessingException e) {            
            log.error(e.getMessage());
            throw new UnsupportedOperationException("Error processing export to json");
        }
        return outData;        
    }
    
    private ConfigData getModelConfigData(String projectVersionId, String modelId) {

        ConfigData configData = new ConfigData();
        
        for (int i = 0; i <= MODEL_SQL.length-1; i++) {
            String[] entry = MODEL_SQL[i];
            
            configData.modelData.add(getConfigTableData(tablePrefix + entry[0],
                    String.format(entry[1],tablePrefix,projectVersionId, modelId),
                    configData));            
        }
        
        return configData;
    }
    
    private ConfigData getResourceConfigData(String projectVersionId, String resourceId) {

        ConfigData configData = new ConfigData();
        for (int i = 0; i <= RESOURCE_SQL.length-1; i++) {
            String[] entry = RESOURCE_SQL[i];
            
            configData.resourceData.add(getConfigTableData(tablePrefix + entry[0],
                    String.format(entry[1],tablePrefix,projectVersionId, resourceId),
                    configData));            
        }

        return configData;
    }    
    
    private ConfigData getFlowConfigData(String projectVersionId, String flowId) {

        ConfigData configData = new ConfigData();
        for (int i = 0; i <= FLOW_SQL.length-1; i++) {
            String[] entry = FLOW_SQL[i];
            
            configData.flowData.add(getConfigTableData(tablePrefix + entry[0],
                    String.format(entry[1],tablePrefix,projectVersionId, flowId),
                    configData));            
        }
        return configData;
    }
    
    private TableData getConfigTableData(String tableName, String sql, ConfigData exportData) {

        ISqlTemplate template = databasePlatform.getSqlTemplate();
        TableData tableData = new TableData(tableName);
        List<Row> rows = template.query(sql);
        excludeColumnData(rows);
        tableData.rows.addAll(rows);
        
        return tableData;
    }
    
    private void excludeColumnData(List<Row> rows) {
        for (Row row : rows) {
            for (int i = 0; i < columnsToExclude.length; i++) {
                row.remove(columnsToExclude[i]);
            }
        }
    }

    @Override
    public void importConfiguration(String configDataString) {

        ConfigData configData = deserializeConfigurationData(configDataString); 
        importConfiguration(configData);
    }  
    
    private void importConfiguration(ConfigData configData) {

        ImportConfigData importData = new ImportConfigData(configData);
        ISqlTransaction transaction = databasePlatform.getSqlTemplate().startSqlTransaction();  
        
        if (importData.getResourceData().size() > 0) {
            importResourceConfiguration(importData, transaction);
        }
        if (importData.getModelData().size() > 0) {
            importModelConfiguration(importData, transaction);
        }
        if (importData.getFlowData().size() > 0) {
            importFlowConfiguration(importData, transaction);
        }                
        processDeletes(importData, transaction);
        
        transaction.commit();

    }
    
    private void processDeletes(ImportConfigData importData, ISqlTransaction transaction) {
        
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_FLOW_STEP_LINK"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_FLOW_STEP"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_FLOW_PARAMETER"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_FLOW"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_COMPONENT_ATTRIBUTE_SETTING"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_COMPONENT_ENTITY_SETTING"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_COMPONENT_SETTING"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_COMPONENT"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_RESOURCE_SETTING"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_RESOURCE"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_MODEL_ATTRIBUTE"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_MODEL_ENTITY"), transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_MODEL"), transaction);        
    }
    
    
    private void importResourceConfiguration(ImportConfigData importData, ISqlTransaction transaction) {
        
        String projectVersionId = (String) importData.getResourceData().get(0).getTableData().get(0).get("PROJECT_VERSION_ID");
        String resourceId = (String) importData.getModelData().get(0).getTableData().get(0).get(RESOURCE_PK);
        ConfigData existingConfigData = getModelConfigData(projectVersionId, resourceId);
   
        TableData existingResourceData = existingConfigData.resourceData.get(0);
        TableData importResourceData = importData.resourceData.get(0);          
        processConfigTableData(importData, existingResourceData, importResourceData, resourceId, transaction);
        
        TableData existingResourceSettingData = existingConfigData.resourceData.get(1);
        TableData importResourceSettingData = importData.resourceData.get(1);                  
        processConfigTableData(importData, existingResourceSettingData, importResourceSettingData, resourceId, transaction);        
        
    }

    private void importModelConfiguration(ImportConfigData importData, ISqlTransaction transaction) {
        
        String projectVersionId = (String) importData.getModelData().get(0).getTableData().get(0).get("PROJECT_VERSION_ID");
        String modelId = (String) importData.getModelData().get(0).getTableData().get(0).get(MODEL_PK);
        ConfigData existingConfigData = getModelConfigData(projectVersionId, modelId);
     
        TableData existingModelData = existingConfigData.modelData.get(0);
        TableData importModelData = importData.modelData.get(0);          
        processConfigTableData(importData, existingModelData, importModelData, modelId, transaction);
        
        TableData existingModelEntityData = existingConfigData.modelData.get(1);
        TableData importModelEntityData = importData.modelData.get(1);                  
        processConfigTableData(importData, existingModelEntityData, importModelEntityData, modelId, transaction);        

        TableData existingModelAttributeData = existingConfigData.modelData.get(2);
        TableData importModelAttributeData = importData.modelData.get(2);          
        processConfigTableData(importData, existingModelAttributeData, importModelAttributeData, modelId, transaction);
    }

    private void importFlowConfiguration(ImportConfigData importData, ISqlTransaction transaction) {

        String projectVersionId = (String) importData.getModelData().get(0).getTableData().get(0).get("PROJECT_VERSION_ID");
        String flowId = (String) importData.getFlowData().get(0).getTableData().get(0).get(FLOW_PK);
        ConfigData existingConfigData = getFlowConfigData(projectVersionId, flowId);
        
        TableData existingComponentData = existingConfigData.flowData.get(0);
        TableData importComponentData = importData.flowData.get(0);          
        processConfigTableData(importData, existingComponentData, importComponentData, flowId, transaction);
        
        
    }
        
    private void processConfigTableData(ImportConfigData configData, TableData existingData, TableData importData, String key,
            ISqlTransaction transaction) {
        
        TableData inserts = findInserts(existingData, importData, key);
        processTableInserts(inserts, transaction);
        
        TableData updates = findUpdates(existingData, importData, key);
        processTableUpdates(updates, transaction);
                
        TableData deletes = findDeletes(existingData, importData, key);
        configData.deletesToProcess.put(importData.getTableName(), deletes);
    }
    
    private void processTableInserts(TableData inserts, ISqlTransaction transaction) {
        
        Table table = databasePlatform.getTableFromCache(null, null, inserts.getTableName(), false);   
        excludeColumns(table);

        DmlStatement stmt = databasePlatform.createDmlStatement(DmlType.INSERT, table.getCatalog(), table.getSchema(), table.getName(), 
                table.getPrimaryKeyColumns(), table.getColumns(), null, null, true);
        for (LinkedCaseInsensitiveMap<Object> row : inserts.getTableData()) {
            transaction.prepareAndExecute(stmt.getSql(),row);
        } 
    }
    
    private void processTableUpdates(TableData updates, ISqlTransaction transaction) {
        Table table = databasePlatform.getTableFromCache(null, null, updates.getTableName(), false);
        excludeColumns(table);

        DmlStatement stmt = databasePlatform.createDmlStatement(DmlType.UPDATE, table.getCatalog(), table.getSchema(), table.getName(), 
                table.getPrimaryKeyColumns(), getUpdateColumns(table), null, null, true);
        for (LinkedCaseInsensitiveMap<Object> row : updates.getTableData()) {
            transaction.prepareAndExecute(stmt.getSql(), row);
        }        
    }

    private void processTableDeletes(TableData deletes, ISqlTransaction transaction) {
        Table table = databasePlatform.getTableFromCache(null, null, deletes.getTableName(), false);
        excludeColumns(table);

        DmlStatement stmt = databasePlatform.createDmlStatement(DmlType.DELETE, table.getCatalog(), table.getSchema(), table.getName(), 
                table.getPrimaryKeyColumns(), getUpdateColumns(table), null, null, true);        
        for (LinkedCaseInsensitiveMap<Object> row : deletes.getTableData()) {
            transaction.prepareAndExecute(stmt.getSql(),row);            
        }
    }    
    
    private Column[] getUpdateColumns(Table table) {
        ArrayList<Column> columns = new ArrayList<Column>();
        for (Column column : table.getColumns()) {
            if (!column.isPrimaryKey())
                columns.add(column);
        }
        return columns.toArray(new Column[0]);
    }
    
    private void excludeColumns(Table table) {
        for (Column column : table.getColumns()) {
            if (column.getName().equalsIgnoreCase(columnsToExclude[0]) ||
                    column.getName().equalsIgnoreCase(columnsToExclude[1]) ||
                    column.getName().equalsIgnoreCase(columnsToExclude[2]) ||
                    column.getName().equalsIgnoreCase(columnsToExclude[3])) {
                table.removeColumn(column);
            }
        }
    }
    
    private ConfigData deserializeConfigurationData(String configDataString) {

        ObjectMapper mapper = new ObjectMapper();
        ConfigData configData = null;
        try {
            configData = mapper.readValue(configDataString, ConfigData.class);
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new UnsupportedOperationException("Error deserializing json data for import");
        }
        return configData;
    }
    
    private TableData findInserts(TableData existingData, TableData newData, String pk) {

        boolean found;        
        TableData inserts = new TableData(newData.tableName);
        for (LinkedCaseInsensitiveMap<Object> newRow : newData.getTableData()) {
            String newPk = (String) newRow.get(pk);
            found = false;
            for (LinkedCaseInsensitiveMap<Object> existingRow : existingData.getTableData()) {                
                String existingPk = (String) existingRow.get(pk);                
                if (newPk.equalsIgnoreCase(existingPk)) {
                    found=true;
                }
            }
            if (!found) {
                inserts.getTableData().add(newRow);
            }
        }
        return inserts;
    }
    
    private TableData findDeletes(TableData existingData, TableData newData, String pk) {

        boolean found;
        TableData deletes = new TableData(newData.tableName);
        for (LinkedCaseInsensitiveMap<Object> existingRow : existingData.getTableData()) {
            String existingPk = (String) existingRow.get(pk);
            found = false;
            for (LinkedCaseInsensitiveMap<Object> newRow : newData.getTableData()) {                
                String newPk = (String) newRow.get(pk);                
                if (newPk.equalsIgnoreCase(existingPk)) {
                    found=true;
                }
            }
            if (!found) {
                deletes.getTableData().add(existingRow);
            }
        }
        return deletes;
    }
    
    private TableData findUpdates(TableData existingData, TableData newData, String pk) {
        
        boolean found;        
        TableData updates = new TableData(newData.tableName);
        for (LinkedCaseInsensitiveMap<Object> newRow : newData.getTableData()) {
            String newPk = (String) newRow.get(pk);
            found = false;
            for (LinkedCaseInsensitiveMap<Object> existingRow : existingData.getTableData()) {                
                String existingPk = (String) existingRow.get(pk);                
                if (newPk.equalsIgnoreCase(existingPk)) {
                    found=true;
                }
            }
            if (found) {
                updates.getTableData().add(newRow);
            }
        }
        return updates;
    }
    
    static class TableData {
        
        String tableName;
        List<LinkedCaseInsensitiveMap<Object>> rows = new ArrayList<LinkedCaseInsensitiveMap<Object>>();
        
        public TableData() {            
        }
        
        public TableData(String tableName) {
            this.tableName = tableName;
        }

        public String getTableName() {
            return tableName;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }

        public List<LinkedCaseInsensitiveMap<Object>> getTableData() {
            return rows;
        }

        public void setTableData(List<LinkedCaseInsensitiveMap<Object>> tableData) {
            this.rows = tableData;
        }
        
    }
    
    static class ConfigData {
        
        List<TableData> resourceData = new ArrayList<TableData>(); 
        List<TableData> modelData = new ArrayList<TableData>();
        List<TableData> flowData = new ArrayList<TableData>();
        
        public ConfigData() {            
        }
        
        public List<TableData> getResourceData() {
            return resourceData;
        }

        public void setResourceData(List<TableData> resourceData) {
            this.resourceData = resourceData;
        }

        public List<TableData> getModelData() {
            return modelData;
        }

        public void setModelData(List<TableData> modelData) {
            this.modelData = modelData;
        }

        public List<TableData> getFlowData() {
            return flowData;
        }

        public void setFlowData(List<TableData> flowData) {
            this.flowData = flowData;
        }
     
    }
    
    static class ImportConfigData extends ConfigData {
        
        public ImportConfigData(ConfigData configData) {
            this.resourceData = configData.resourceData;
            this.modelData = configData.modelData;
            this.flowData = configData.flowData;
            this.deletesToProcess = new HashMap<String, TableData>();
        }
        
        Map<String, TableData> deletesToProcess;
        
    }
}
