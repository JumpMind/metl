package org.jumpmind.metl.core.persist;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ImportExportService extends AbstractService implements IImportExportService {

    private IDatabasePlatform databasePlatform;
    private String tablePrefix;

    public ImportExportService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {

        super(persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
        this.tablePrefix = tablePrefix;
    }

    
    @Override
    public String export(ProjectVersion projectVersion, Model model) {
        
        final String MODEL_SQL = "SELECT * FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND ID='%3$s' ORDER BY ID";
        final String MODEL_ENTITY_SQL = "SELECT * FROM %1$s_MODEL_ENTITY ME INNER JOIN %1$s_MODEL M ON ME.MODEL_ID = M.ID " +
                "WHERE M.PROJECT_VERSION_ID='%2$s' AND M.ID='%3$s' ORDER BY ID";
        final String MODEL_ATTRIBUTE_SQL = "SELECT * FROM %1$s_MODEL_ATTRIBUTE MA INNER JOIN %1$s_MODEL_ENTITY ME ON MA.ENTITY_ID = ME.ID INNER JOIN %1$s_MODEL M ON ME.MODEL_ID = M.ID " +
                "WHERE M.PROJECT_VERSION_ID='%2$s' AND M.ID='%3$s' ORDER BY ID";

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        ExportData exportData = new ExportData(ExportLevel.MODEL);
        
        TableData modelData = new TableData(tablePrefix + "model");
        List<Row> rows = template.query(String.format(MODEL_SQL,tablePrefix,projectVersion.getId(), model.getId()));
        modelData.tableData.addAll(rows);
        exportData.tableData.add(modelData);
        
        TableData modelEntityData = new TableData(tablePrefix + "model_entity");
        modelEntityData.tableData.addAll(template.query(String.format(MODEL_ENTITY_SQL,tablePrefix,projectVersion.getId(), model.getId())));
        exportData.tableData.add(modelEntityData);

        TableData modelAttributeData = new TableData(tablePrefix + "model_attribute");
        modelAttributeData.tableData.addAll(template.query(String.format(MODEL_ATTRIBUTE_SQL,tablePrefix,projectVersion.getId(), model.getId())));
        exportData.tableData.add(modelAttributeData);

        String out;
        try {
            out = mapper.writeValueAsString(exportData);
        } catch (JsonProcessingException e) {            
            log.error(e.getMessage());
            throw new UnsupportedOperationException("Error processing export to json");
        }
        return out;
    }
    
    private enum ExportLevel {PROJECT, FLOW, MODEL}
    
    class TableData {
        String tableName;
        List<LinkedCaseInsensitiveMap<Object>> tableData = new ArrayList<LinkedCaseInsensitiveMap<Object>>();
        
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
            return tableData;
        }

        public void setTableData(List<LinkedCaseInsensitiveMap<Object>> tableData) {
            this.tableData = tableData;
        }
        
    }
    
    class ExportData {
        
        ExportLevel exportLevel;
        List<TableData> tableData = new ArrayList<TableData>();
        
        public ExportData(ExportLevel exportLevel) {
            this.exportLevel = exportLevel;
        }

        public ExportLevel getExportLevel() {
            return exportLevel;
        }

        public void setExportLevel(ExportLevel exportLevel) {
            this.exportLevel = exportLevel;
        }

        public List<TableData> getTableData() {
            return tableData;
        }

        public void setTableData(List<TableData> tableData) {
            this.tableData = tableData;
        }        
    }
}
