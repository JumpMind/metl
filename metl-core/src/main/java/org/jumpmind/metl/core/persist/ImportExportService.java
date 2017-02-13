package org.jumpmind.metl.core.persist;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDependency;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.ReleasePackageProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.core.security.SecurityConstants;
import org.jumpmind.metl.core.util.MessageException;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.util.AppUtils;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ImportExportService extends AbstractService implements IImportExportService {
    
    final static Integer TABLE = new Integer(0);
    final static Integer SQL = new Integer(1);
    final static Integer KEY_COLUMNS = new Integer(2);
    
    final static Integer PROJECT_IDX = new Integer(0);
    final static Integer PROJECT_VERSION_IDX = new Integer(1);
    final static Integer MODEL_IDX = new Integer(0);
    final static Integer RESOURCE_IDX = new Integer(0);
    final static Integer FLOW_IDX = new Integer(4);
    final static Integer RELEASE_PACKAGE_IDX = new Integer(0);
    
    final static Integer CREATE_TIME_IDX = new Integer(0);
    final static Integer LAST_UPDATE_TIME_IDX = new Integer(1);
    final static Integer CREATE_BY_IDX = new Integer(2);
    final static Integer LAST_UPDATE_BY_IDX = new Integer(3);

    final String[][] RELEASE_PACKAGE_SQL = {
            {"_RELEASE_PACKAGE","SELECT * FROM %1$s_RELEASE_PACKAGE WHERE ID='%2$s' ORDER BY ID", "ID"},
            {"_RELEASE_PACKAGE_PROJECT_VERSION","SELECT * FROM %1$s_RELEASE_PACKAGE_PROJECT_VERSION WHERE " +
                    "RELEASE_PACKAGE_ID='%2$s' ORDER BY RELEASE_PACKAGE_ID", "RELEASE_PACKAGE_ID,PROJECT_VERSION_ID"}
    };

    final String[][] PROJECT_SQL = {
            {"_PROJECT","SELECT * FROM %1$s_PROJECT WHERE ID IN (SELECT PROJECT_ID FROM %1$s_PROJECT_VERSION WHERE ID='%2$s') UNION SELECT * FROM %1$s_PROJECT WHERE ID='%3$s' ORDER BY ID","ID"},
            {"_PROJECT_VERSION","SELECT * FROM %1$s_PROJECT_VERSION WHERE ID='%2$s' ORDER BY ID","ID"},
            {"_PROJECT_VERSION_DEFINITION_PLUGIN","SELECT * FROM %1$s_PROJECT_VERSION_DEFINITION_PLUGIN WHERE PROJECT_VERSION_ID='%2$s' ORDER BY PROJECT_VERSION_ID","PROJECT_VERSION_ID,COMPONENT_TYPE_ID"},           
            {"_PROJECT_VERSION_DEPENDENCY","SELECT * FROM %1$s_PROJECT_VERSION_DEPENDENCY WHERE PROJECT_VERSION_ID='%2$s' ORDER BY ID","ID"}
    };
    
    final String[][] MODEL_SQL = {
            {"_MODEL","SELECT * FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND ID='%3$s' ORDER BY ID","ID"},
            {"_MODEL_ENTITY","SELECT * FROM %1$s_MODEL_ENTITY WHERE MODEL_ID='%3$s' ORDER BY ID","ID"},
            {"_MODEL_ATTRIBUTE","SELECT * FROM %1$s_MODEL_ATTRIBUTE WHERE ENTITY_ID IN "
            + "(SELECT ID FROM %1$s_MODEL_ENTITY WHERE MODEL_ID IN "
            + "(SELECT ID FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND ID='%3$s')) ORDER BY ID","ID"}
    };    
    
    final String[][] RESOURCE_SQL = {
            {"_RESOURCE","SELECT * FROM %1$s_RESOURCE WHERE PROJECT_VERSION_ID = '%2$s' AND ID='%3$s' ORDER BY ID","ID"},
            {"_RESOURCE_SETTING","SELECT * FROM %1$s_RESOURCE_SETTING WHERE RESOURCE_ID='%3$s' ORDER BY RESOURCE_ID, NAME","RESOURCE_ID,NAME"}
    };
    
    final String[][] FLOW_SQL = {
            {"_COMPONENT","SELECT * FROM %1$s_COMPONENT WHERE PROJECT_VERSION_ID='%2$s' AND ID IN "
                    + "(SELECT DISTINCT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s') ORDER BY ID", "ID"},
            {"_COMPONENT_SETTING","SELECT * FROM %1$s_COMPONENT_SETTING WHERE COMPONENT_ID IN "
                    + "(SELECT DISTINCT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s') ORDER BY ID", "ID"},
            {"_COMPONENT_ENTITY_SETTING","SELECT * FROM %1$s_COMPONENT_ENTITY_SETTING WHERE COMPONENT_ID IN "
                    + "(SELECT DISTINCT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s') ORDER BY ID", "ID"},
            {"_COMPONENT_ATTRIBUTE_SETTING","SELECT * FROM %1$s_COMPONENT_ATTRIBUTE_SETTING WHERE COMPONENT_ID IN "
                    + "(SELECT DISTINCT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s') ORDER BY ID", "ID"},
            {"_FLOW","SELECT * FROM %1$s_FLOW WHERE PROJECT_VERSION_ID='%2$s' AND ID='%3$s' ORDER BY ID", "ID"},
            {"_FLOW_PARAMETER","SELECT * FROM %1$s_FLOW_PARAMETER WHERE FLOW_ID='%3$s' ORDER BY ID", "ID"},
            {"_FLOW_STEP","SELECT * FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s' ORDER BY ID", "ID"},
            {"_FLOW_STEP_LINK","SELECT * FROM %1$s_FLOW_STEP_LINK WHERE SOURCE_STEP_ID IN "
                    + "(SELECT DISTINCT ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%3$s') ORDER BY SOURCE_STEP_ID, TARGET_STEP_ID", "SOURCE_STEP_ID,TARGET_STEP_ID"}            
    };
    
    private IDatabasePlatform databasePlatform;
    private IConfigurationService configurationService;
    private ISecurityService securityService;
    private String tablePrefix;
    private String[] columnsToExclude;
    private Set<String> importsToAudit = new HashSet<>();
    private Set<String> projectsExported = new HashSet<>();

    public ImportExportService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix,
            IConfigurationService configurationService, ISecurityService securityService) {
        super(persistenceManager, tablePrefix);
        this.securityService = securityService;
        this.databasePlatform = databasePlatform;
        this.configurationService = configurationService;
        this.tablePrefix = tablePrefix;
        importsToAudit.add(tableName(Project.class).toUpperCase());
        importsToAudit.add(tableName(ProjectVersion.class).toUpperCase());
        importsToAudit.add(tableName(Flow.class).toUpperCase());
        importsToAudit.add(tableName(Model.class).toUpperCase());
        importsToAudit.add(tableName(Resource.class).toUpperCase());
        setColumnsToExclude();
    }

    private void setColumnsToExclude() {
        columnsToExclude = new String[4];
        columnsToExclude[CREATE_TIME_IDX] = "CREATE_TIME";
        columnsToExclude[LAST_UPDATE_TIME_IDX] = "LAST_UPDATE_TIME";
        columnsToExclude[CREATE_BY_IDX] = "CREATE_BY";
        columnsToExclude[LAST_UPDATE_BY_IDX] = "LAST_UPDATE_BY";
    }
    
    @Override
    public String exportProjectVersion(String projectVersionId, String userId) {
        projectsExported.clear();
        List<FlowName> flows = new ArrayList<>();
        flows.addAll(configurationService.findFlowsInProject(projectVersionId, true));
        flows.addAll(configurationService.findFlowsInProject(projectVersionId, false));
        List<String> flowIds = new ArrayList<>();
        for (FlowName flowName : flows) {
            flowIds.add(flowName.getId());
        }

        List<ModelName> models = configurationService.findModelsInProject(projectVersionId);
        List<String> modelIds = new ArrayList<>();
        for (ModelName modelName : models) {
            modelIds.add(modelName.getId());
        }

        List<ResourceName> resources = configurationService
                .findResourcesInProject(projectVersionId);
        List<String> resourceIds = new ArrayList<>();
        for (ResourceName resource : resources) {
            resourceIds.add(resource.getId());
        }
        return exportFlows(projectVersionId, flowIds, modelIds, resourceIds, userId);
    }

    @Override
    public String exportReleasePackage(String releasePackageId, String userId) {        
        projectsExported.clear();
        ConfigData exportData = initExport();
        ReleasePackage releasePackage = configurationService.findReleasePackage(releasePackageId);
        
        DirectedAcyclicGraph<String, DefaultEdge> dag = new DirectedAcyclicGraph<String, DefaultEdge>(DefaultEdge.class);
        for (ReleasePackageProjectVersion rppv : releasePackage.getProjectVersions()) {
            dag.addVertex(rppv.getProjectVersionId());
            List<ProjectVersionDependency> dependencies = configurationService.findProjectDependencies(rppv.getProjectVersionId());
            for (ProjectVersionDependency dependency: dependencies) {
                dag.addVertex(dependency.getTargetProjectVersionId());
                dag.addEdge(dependency.getTargetProjectVersionId(), rppv.getProjectVersionId());
            }
        }      
        
        //add projects to the export in topological order
        Iterator<String> itr = dag.iterator();
        while (itr.hasNext()) {                        
            String projectVersionId = itr.next();
            initProjectVersionExport(exportData, projectVersionId);            
            Set<String> flowIds = new HashSet<String>();
            Set<String> modelIds = new HashSet<String>();
            Set<String> resourceIds = new HashSet<String>();               
            flowIds.addAll(convertAbstractNamesToIds(configurationService.findFlowsInProject(projectVersionId, false)));
            modelIds.addAll(convertAbstractNamesToIds(configurationService.findModelsInProject(projectVersionId)));
            resourceIds.addAll(convertAbstractNamesToIds(configurationService.findResourcesInProject(projectVersionId)));   
            addProjectVersionToConfigData(projectVersionId, exportData, flowIds, modelIds, resourceIds);
            
            save(new AuditEvent(AuditEvent.EventType.EXPORT, String.format("%s, flows: %d, models %d, resources: %d", 
                    releasePackage.getName(), flowIds.size(), modelIds.size(), resourceIds.size()), userId));
        }
        
        addConfigData(exportData.getReleasePackageData(), RELEASE_PACKAGE_SQL, releasePackageId, releasePackageId);

        return serializeExportToJson(exportData);
    }

    protected Set<String> convertAbstractNamesToIds(List<? extends AbstractObject> objectNames) {
        Set<String> ids = new HashSet<String>();
        for (AbstractObject objectName : objectNames) {
            ids.add(objectName.getId());
        }
        return ids;
    }
    
    protected void addProjectVersionToConfigData(String projectVersionId,ConfigData exportData,
            Set<String> flowIds, Set<String> modelIds, Set<String> resourceIds) {
        
        ProjectVersion version = configurationService.findProjectVersion(projectVersionId);
        ProjectVersionData projectVersionData = exportData.getProjectVersionData().get(exportData.getProjectVersionData().size()-1);
        
        addConfigData(projectVersionData.getProjectData(), PROJECT_SQL, projectVersionId, null);            
        projectsExported.add(version.getProjectId());
        for (String flowId : flowIds) {
            addConfigData(projectVersionData.getFlowData(), FLOW_SQL, projectVersionId, flowId);
        }
        for (String modelId : modelIds) {
            addConfigData(projectVersionData.getModelData(), MODEL_SQL, projectVersionId, modelId);
        }
        for (String resourceId : resourceIds) {
            addConfigData(projectVersionData.getResourceData(), RESOURCE_SQL, projectVersionId, resourceId);
        }        
    }
    
    @Override
    public String exportFlows(String projectVersionId, List<String> flowIds, List<String> modelIds,
            List<String> resourceIds, String userId) {     
        
        ProjectVersion version = configurationService.findProjectVersion(projectVersionId);
        save(new AuditEvent(AuditEvent.EventType.EXPORT, String.format("%s, flows: %d, models %d, resources: %d", 
                version.getName(), flowIds.size(), modelIds.size(), resourceIds.size()), userId));
        ConfigData exportData = initExport();
        initProjectVersionExport(exportData, projectVersionId);
        addProjectVersionToConfigData(projectVersionId, exportData, new HashSet<String>(flowIds), new HashSet<String>(modelIds), new HashSet<String>(resourceIds));
        
        return serializeExportToJson(exportData);
    }

    protected ConfigData initExport() {
        ConfigData exportData = new ConfigData();
        exportData.setHostName(AppUtils.getHostName());
        exportData.setVersionNumber(VersionUtils.getCurrentVersion());
        initConfigData(exportData.getReleasePackageData(), RELEASE_PACKAGE_SQL);

        return exportData;        
    }
    
    protected void initProjectVersionExport(ConfigData exportData, String projectVersionId) {
        ProjectVersionData projectVersionData = new ProjectVersionData();
        projectVersionData.setProjectVersionId(projectVersionId);
        
        initConfigData(projectVersionData.getProjectData(), PROJECT_SQL);
        initConfigData(projectVersionData.getModelData(), MODEL_SQL);
        initConfigData(projectVersionData.getResourceData(), RESOURCE_SQL);
        initConfigData(projectVersionData.getFlowData(), FLOW_SQL);
        
        exportData.getProjectVersionData().add(projectVersionData);
    }
    
    @Override
    public void importConfiguration(String configDataString, String userId) {
        ConfigData configData = deserializeConfigurationData(configDataString);
        importConfiguration(configData, userId);
    }

    private String serializeExportToJson(ConfigData exportData) {
        ObjectMapper mapper = new ObjectMapper();        
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        mapper.setDateFormat(df);
        String outData;

        try {
            outData = mapper.writeValueAsString(exportData);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new UnsupportedOperationException("Error processing export to json");
        }
        return outData;
    }

    private void addConfigData(List<TableData> tableData, String[][] sqlElements,
            String projectVersionId, String keyValue) {
        
        ProjectVersion version = configurationService.findProjectVersion(projectVersionId);
        
        for (int i = 0; i <= sqlElements.length - 1; i++) {
            //TODO greg finish this
            if (!sqlElements[0][0].equalsIgnoreCase("_PROJECT") ||
                    !projectsExported.contains(version.getProjectId()) ) {                
                String[] entry = sqlElements[i];
                List<Row> rows = getConfigTableData(String.format(entry[SQL], 
                        tablePrefix, projectVersionId, keyValue));
                for (Row row : rows) {
                    if (isPassword(row.getString("NAME", false))) {
                        String value = row.getString("VALUE", false);
                        if (isNotBlank(value)) {
                            if (value.startsWith(SecurityConstants.PREFIX_ENC)) {
                                try {
                                    row.put("VALUE", securityService.decrypt(
                                            value.substring(SecurityConstants.PREFIX_ENC.length() - 1)));
                                } catch (Exception e) {
                                }
                            }
                        }
                    }
                    tableData.get(i).rows.put(getPkDataAsString(row, entry[KEY_COLUMNS]), row);
                }
            }
        }
        projectsExported.add(version.getProjectId());
    }

    private List<Row> getConfigTableData(String sql) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> rows = template.query(sql);
        excludeColumnData(rows);

        return rows;
    }

    private void excludeColumnData(List<Row> rows) {
        for (Row row : rows) {
            for (int i = 0; i < columnsToExclude.length; i++) {
                row.remove(columnsToExclude[i]);
            }
        }
    }

    private void importConfiguration(ConfigData configData, String userId) {
        ImportConfigData importData = new ImportConfigData(configData);
        ISqlTransaction transaction = databasePlatform.getSqlTemplate().startSqlTransaction();
        try {
            for (ProjectVersionData data : importData.getProjectVersionData()) {
                String projectVersionId = data.getProjectVersionId();
                if (data.getProjectData().size() > 0
                        && data.getProjectData().get(PROJECT_IDX).rows.size() > 0) {
                    importProjectConfiguration(projectVersionId, importData, transaction, userId);
                }
                if (data.getResourceData().size() > 0
                        && data.getResourceData().get(RESOURCE_IDX).rows.size() > 0) {
                    importResourceConfiguration(projectVersionId, importData, transaction, userId);
                }
                if (data.getModelData().size() > 0
                        && data.getModelData().get(MODEL_IDX).rows.size() > 0) {
                    importModelConfiguration(projectVersionId, importData, transaction, userId);
                }
                if (data.getFlowData().size() > 0
                        && data.getFlowData().get(FLOW_IDX).rows.size() > 0) {
                    importFlowConfiguration(projectVersionId, importData, transaction, userId);
                }
                processDeletes(importData, transaction);                
            }
            importReleasePackageConfiguration(importData, transaction, userId);
            transaction.commit();

        } catch (Exception e) {
            String msg =  String.format("Failed to import from host: %s from Metl version: %s",
                    configData.getHostName(), configData.getVersionNumber());
            transaction.rollback();
            save(new AuditEvent(AuditEvent.EventType.IMPORT, msg, userId));
            rethrow(e);
        }

    }

    private void processDeletes(ImportConfigData importData, ISqlTransaction transaction) {
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_FLOW_STEP_LINK"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_FLOW_STEP"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_FLOW_PARAMETER"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_FLOW"), transaction);
        processTableDeletes(
                importData.deletesToProcess.get(tablePrefix + "_COMPONENT_ATTRIBUTE_SETTING"),
                transaction);
        processTableDeletes(
                importData.deletesToProcess.get(tablePrefix + "_COMPONENT_ENTITY_SETTING"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_COMPONENT_SETTING"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_COMPONENT"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_RESOURCE_SETTING"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_RESOURCE"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_MODEL_ATTRIBUTE"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_MODEL_ENTITY"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_MODEL"), transaction);
    }

    private void importReleasePackageConfiguration(ImportConfigData importData,
            ISqlTransaction transaction, String userId) {

        List<TableData> existingReleasePackageData = new ArrayList<TableData>();
        initConfigData(existingReleasePackageData, RELEASE_PACKAGE_SQL);        
        
        Iterator<String> itr = importData.getReleasePackageData().get(RELEASE_PACKAGE_IDX)
                .getTableData().keySet().iterator();
        while (itr.hasNext()) {
            String key = itr.next();
            LinkedCaseInsensitiveMap<Object> row = importData.getReleasePackageData().get(RELEASE_PACKAGE_IDX).getTableData().get(key);
            addConfigData(existingReleasePackageData, RELEASE_PACKAGE_SQL, (String) row.get(RELEASE_PACKAGE_SQL[RELEASE_PACKAGE_IDX][KEY_COLUMNS]),
                    (String) row.get(RELEASE_PACKAGE_SQL[RELEASE_PACKAGE_IDX][KEY_COLUMNS]));
        }
        
        for (int i = 0; i <= RELEASE_PACKAGE_SQL.length - 1; i++) {
            if (importData.releasePackageData.size() > i) {
                TableData importReleasePackageData = importData.releasePackageData.get(i);
                try {
                    processConfigTableData(importData, existingReleasePackageData.get(i), importReleasePackageData,
                            RELEASE_PACKAGE_SQL[i][KEY_COLUMNS], transaction, userId);
                } catch (RuntimeException e) {
                    throw e;
                }
            }
        }   
    }
     
    private void importProjectConfiguration(String projectVersionId, ImportConfigData importData,
            ISqlTransaction transaction, String userId) {        
        List<TableData> existingProjectData = new ArrayList<TableData>();
        initConfigData(existingProjectData, PROJECT_SQL);
        ProjectVersionData data = importData.getProjectVersion(projectVersionId);
        
        Iterator<String> itr = data.getProjectData().get(PROJECT_IDX)
                .getTableData().keySet().iterator();
        while (itr.hasNext()) {
            String key = itr.next();
            LinkedCaseInsensitiveMap<Object> row = data.getProjectData().get(PROJECT_IDX).getTableData().get(key);
            addConfigData(existingProjectData, PROJECT_SQL, projectVersionId,
                    (String) row.get(PROJECT_SQL[PROJECT_IDX][KEY_COLUMNS]));
        }
        
        for (int i = 0; i <= PROJECT_SQL.length - 1; i++) {
            if (data.projectData.size() > i) {
                TableData importProjectData = data.projectData.get(i);
                try {
                    processConfigTableData(importData, existingProjectData.get(i), importProjectData,
                            PROJECT_SQL[i][KEY_COLUMNS], transaction, userId);
                } catch (RuntimeException e) {
                    if (importProjectData.getTableName().toLowerCase().endsWith("project_version_dependency")) {
                        Collection<LinkedCaseInsensitiveMap<Object>> maps = importProjectData.getTableData().values();
                        StringBuilder ids = new StringBuilder();
                        for (LinkedCaseInsensitiveMap<Object> linkedCaseInsensitiveMap : maps) {
                            if (ids.length() > 0) {
                                ids.append(",");
                            }
                            ids.append(linkedCaseInsensitiveMap.get("TARGET_PROJECT_VERSION_ID"));
                        }
                        throw new MessageException("Missing dependent project.  Please load projects with the following ids first: " + ids); 
                    } else {
                        throw e;
                    }
                }
            }
        }   
    }

    private void importResourceConfiguration(String projectVersionId, ImportConfigData importData,
            ISqlTransaction transaction, String userId) {
        List<TableData> existingResourceData = new ArrayList<TableData>();
        initConfigData(existingResourceData, RESOURCE_SQL);
        ProjectVersionData data = importData.getProjectVersion(projectVersionId);
        
        Iterator<String> itr = data.getResourceData().get(RESOURCE_IDX)
                .getTableData().keySet().iterator();        
        while (itr.hasNext()) {
            String key = itr.next();
            LinkedCaseInsensitiveMap<Object> row = data.getResourceData().get(RESOURCE_IDX).getTableData().get(key);
            addConfigData(existingResourceData, RESOURCE_SQL, projectVersionId,
                    (String) row.get(RESOURCE_SQL[RESOURCE_IDX][KEY_COLUMNS]));
        }       
        
        for (int i = 0; i <= RESOURCE_SQL.length - 1; i++) {
            TableData importResourceData = data.resourceData.get(i);
            processConfigTableData(importData, existingResourceData.get(i), importResourceData,
                    RESOURCE_SQL[i][KEY_COLUMNS], transaction, userId);
        }
    }

    private void importModelConfiguration(String projectVersionId, ImportConfigData importData,
            ISqlTransaction transaction, String userId) {
        List<TableData> existingModelData = new ArrayList<TableData>();
        initConfigData(existingModelData, MODEL_SQL);
        ProjectVersionData data = importData.getProjectVersion(projectVersionId);
        
        Iterator<String> itr = data.getModelData().get(MODEL_IDX)
                .getTableData().keySet().iterator();        
        while (itr.hasNext()) {
            String key = itr.next();
            LinkedCaseInsensitiveMap<Object> row = data.getModelData().get(MODEL_IDX).getTableData().get(key);
            addConfigData(existingModelData, MODEL_SQL, projectVersionId,
                    (String) row.get(MODEL_SQL[MODEL_IDX][KEY_COLUMNS]));
        }
        
        for (int i = 0; i <= MODEL_SQL.length - 1; i++) {
            TableData importModelData = data.modelData.get(i);
            processConfigTableData(importData, existingModelData.get(i), importModelData,
                    MODEL_SQL[i][KEY_COLUMNS], transaction, userId);
        }
    }

    private void importFlowConfiguration(String projectVersionId, ImportConfigData importData, ISqlTransaction transaction, String userId) {
        List<TableData> existingFlowData = new ArrayList<TableData>();
        initConfigData(existingFlowData, FLOW_SQL);
        ProjectVersionData data = importData.getProjectVersion(projectVersionId);
                
        Iterator<String> itr = data.getFlowData().get(FLOW_IDX)
                .getTableData().keySet().iterator();        
        while (itr.hasNext()) {
            String key = itr.next();
            LinkedCaseInsensitiveMap<Object> row = data.getFlowData().get(FLOW_IDX).getTableData().get(key);
            addConfigData(existingFlowData, FLOW_SQL, projectVersionId,
                    (String) row.get(FLOW_SQL[FLOW_IDX][KEY_COLUMNS]));
        }  

        for (int i = 0; i <= FLOW_SQL.length - 1; i++) {
            TableData importFlowData = data.flowData.get(i);
            processConfigTableData(importData, existingFlowData.get(i), importFlowData,
                    FLOW_SQL[i][KEY_COLUMNS], transaction, userId);
        }
    }
   
    private final boolean isPassword(String name) {
        return isNotBlank(name) && name.contains("password");
    }
    
    private void processConfigTableData(ImportConfigData configData, TableData existingData,
            TableData importData, String primaryKeyColumns, ISqlTransaction transaction, String userId) {        
        if (importsToAudit.contains(importData.getTableName().toUpperCase())) {
            for (LinkedCaseInsensitiveMap<Object> row : importData.getTableData().values()) {
                String name = (String) row.get("name");                
                if (name == null) {
                    name = (String) row.get("version_label");
                }
                save(new AuditEvent(AuditEvent.EventType.IMPORT,
                        String.format("%s: %s from host: %s from Metl version: %s", importData.getTableName(), name, configData.getHostName(), 
                                configData.getVersionNumber()), userId));
            }
        }
        
        for (LinkedCaseInsensitiveMap<Object> row : importData.getTableData().values()) {
            if (isPassword((String)row.get("NAME"))) {
                String value = (String)row.get("VALUE");
                if (isNotBlank(value)) {
                    if (!value.startsWith(SecurityConstants.PREFIX_ENC)) {
                        row.put("VALUE",
                                SecurityConstants.PREFIX_ENC + securityService.encrypt(value));
                    }
                }
            }
        }
        
        TableData inserts = findInserts(existingData, importData, primaryKeyColumns);
        processTableInserts(inserts, transaction);

        TableData updates = findUpdates(existingData, importData, primaryKeyColumns);
        processTableUpdates(updates, transaction);

        TableData deletes = findDeletes(existingData, importData, primaryKeyColumns);
        configData.deletesToProcess.put(importData.getTableName(), deletes);
    }

    private void processTableInserts(TableData inserts, ISqlTransaction transaction) {
            Table table = databasePlatform.getTableFromCache(null, null, inserts.getTableName(),
                    false);
            excludeInsertColumns(table);
            DmlStatement stmt = databasePlatform.createDmlStatement(DmlType.INSERT,
                    table.getCatalog(), table.getSchema(), table.getName(),
                    table.getPrimaryKeyColumns(), table.getColumns(), null, null, true);
            
            Iterator<String> itr = inserts.getTableData().keySet().iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                LinkedCaseInsensitiveMap<Object> row = inserts.getTableData().get(key);
                Date createTime = new Date();
                row.put("CREATE_TIME", createTime);
                row.put("LAST_UPDATE_TIME", createTime);
                useDefaultsForMissingRequiredColumns(table, row);                
                transaction.prepareAndExecute(stmt.getSql(), row);
            }              

    }

    private void processTableUpdates(TableData updates, ISqlTransaction transaction) {
            Table table = databasePlatform.getTableFromCache(null, null, updates.getTableName(), false);
            excludeUpdateColumns(table);
            DmlStatement stmt = databasePlatform.createDmlStatement(DmlType.UPDATE, table.getCatalog(),
                    table.getSchema(), table.getName(), table.getPrimaryKeyColumns(),
                    getUpdateColumns(table), null, null, true);
            
            Iterator<String> itr = updates.getTableData().keySet().iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                LinkedCaseInsensitiveMap<Object> row = updates.getTableData().get(key);
                row.put("LAST_UPDATE_TIME", new Date());
                useDefaultsForMissingRequiredColumns(table, row);                
                transaction.prepareAndExecute(stmt.getSql(), row);
            }
    }

    private void useDefaultsForMissingRequiredColumns(Table table,LinkedCaseInsensitiveMap<Object> row) {        
        for (Column column : table.getColumnsAsList()) {
            if (!row.containsKey(column.getName())) {
                row.put(column.getName(), column.getDefaultValue());
            }
        }
    }
    
    private void processTableDeletes(TableData deletes, ISqlTransaction transaction) {
        if (deletes != null) {
            Table table = databasePlatform.getTableFromCache(null, null, deletes.getTableName(),
                    false);
            DmlStatement stmt = databasePlatform.createDmlStatement(DmlType.DELETE,
                    table.getCatalog(), table.getSchema(), table.getName(),
                    table.getPrimaryKeyColumns(), getUpdateColumns(table), null, null, true);
            
            Iterator<String> itr = deletes.getTableData().keySet().iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                LinkedCaseInsensitiveMap<Object> row = deletes.getTableData().get(key);
                transaction.prepareAndExecute(stmt.getSql(), row);
            }            
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

    private void excludeInsertColumns(Table table) {
        for (Column column : table.getColumns()) {
            if (column.getName().equalsIgnoreCase(columnsToExclude[CREATE_BY_IDX])
                    || column.getName().equalsIgnoreCase(columnsToExclude[LAST_UPDATE_BY_IDX])) {
                table.removeColumn(column);
            }
        }
    }

    private void excludeUpdateColumns(Table table) {
        for (Column column : table.getColumns()) {
            if (column.getName().equalsIgnoreCase(columnsToExclude[CREATE_BY_IDX])
                    || column.getName().equalsIgnoreCase(columnsToExclude[LAST_UPDATE_BY_IDX])
                    || column.getName().equalsIgnoreCase(columnsToExclude[CREATE_TIME_IDX])) {
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

    private TableData findInserts(TableData existingData, TableData newData,
            String primaryKeyColumns) {
        boolean found;
        TableData inserts = new TableData(newData.tableName);
        
        Iterator<String> newRowItr = newData.getTableData().keySet().iterator();
        while (newRowItr.hasNext()) {
            String newPk = newRowItr.next();
            LinkedCaseInsensitiveMap<Object> newRow = newData.getTableData().get(newPk);
            found = false;
            
            Iterator<String> existingRowItr = existingData.getTableData().keySet().iterator();
            while (existingRowItr.hasNext()) {
                String existingPk = existingRowItr.next();
                if (newPk.equalsIgnoreCase(existingPk)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                inserts.getTableData().put(newPk, newRow);
            }
        }        
        return inserts;
    }

    private TableData findDeletes(TableData existingData, TableData newData,
            String primaryKeyColumns) {
        boolean found;
        TableData deletes = new TableData(newData.tableName);
        
        Iterator<String> existingRowItr = existingData.getTableData().keySet().iterator();
        while (existingRowItr.hasNext()) {
            String existingPk = existingRowItr.next();
            LinkedCaseInsensitiveMap<Object> existingRow = existingData.getTableData().get(existingPk);
            found = false;
            
            Iterator<String> newRowItr = newData.getTableData().keySet().iterator();
            while (newRowItr.hasNext()) {
                String newPk = newRowItr.next();
                if (newPk.equalsIgnoreCase(existingPk)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                deletes.getTableData().put(existingPk, existingRow);
            }            
        }               
        return deletes;
    }

    private TableData findUpdates(TableData existingData, TableData newData,
            String primaryKeyColumns) {
        boolean found;
        String[] pkCols = StringUtils.split(primaryKeyColumns);
        TableData updates = new TableData(newData.tableName);
        // if the pk is the entire record, don't do an update
        
        Iterator<String> itr = existingData.getTableData().keySet().iterator();
        int size=0;
        while (itr.hasNext()) {
            String key = itr.next();
            size = existingData.getTableData().get(key).size();
        }
                
        if (existingData.rows.size() > 0 && pkCols.length + 1 < size) {
            
            Iterator<String> newRowItr = newData.getTableData().keySet().iterator();
            while (newRowItr.hasNext()) {
                String newPk = newRowItr.next();
                LinkedCaseInsensitiveMap<Object> newRow = newData.getTableData().get(newPk);
                found = false;
                
                Iterator<String> existingRowItr = existingData.getTableData().keySet().iterator();
                while (existingRowItr.hasNext()) {
                    String existingPk = existingRowItr.next();
                    if (newPk.equalsIgnoreCase(existingPk)) {
                        found = true;
                        break;
                    }
                }
                if (found) {
                    updates.getTableData().put(newPk, newRow);
                }               
            }            
        }
        return updates;
    }

    private String getPkDataAsString(LinkedCaseInsensitiveMap<Object> row,
            String primaryKeyColumns) {
        StringBuilder pkDataAsString = new StringBuilder();
        String[] pkCols = StringUtils.split(primaryKeyColumns, ',');
        for (int i = 0; i < pkCols.length; i++) {
            pkDataAsString.append(row.get(pkCols[i]));
        }

        return pkDataAsString.toString();
    }

    private void initConfigData(List<TableData> tableData, String[][] sqlElements) {
        for (int i = 0; i <= sqlElements.length - 1; i++) {
            tableData.add(new TableData(tablePrefix + sqlElements[i][0]));
        }
    }

    static class TableData {

        String tableName;
        Map<String, LinkedCaseInsensitiveMap<Object>> rows = new HashMap<String, LinkedCaseInsensitiveMap<Object>>();

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

        public Map<String, LinkedCaseInsensitiveMap<Object>> getTableData() {
            return rows;
        }

        public void setTableData(Map<String, LinkedCaseInsensitiveMap<Object>> tableData) {
            this.rows = tableData;
        }
    }

    static class ConfigData {

        String versionNumber;
        String hostName;
        List<TableData> releasePackageData;
        List<ProjectVersionData> projectVersionData;

        public ConfigData() {
            releasePackageData = new ArrayList<TableData>();
            projectVersionData = new ArrayList<ProjectVersionData>();
        }
        
        public List<TableData> getReleasePackageData() {
            return releasePackageData;
        }

        public ProjectVersionData getProjectVersion(String projectVersionId) {
            for (ProjectVersionData data : projectVersionData) {
                if (projectVersionId.equalsIgnoreCase(data.getProjectVersionId())) {
                    return data;
                }
            }
            return null;
        }
        
        public void setReleasePackageData(List<TableData> releasePackageData) {
            this.releasePackageData = releasePackageData;
        }
        
        public List<ProjectVersionData> getProjectVersionData() {
            return projectVersionData;
        }

        public void setProjectVersionData(List<ProjectVersionData> projectVersionData) {
            this.projectVersionData = projectVersionData;
        }

        public void setVersionNumber(String versionNumber) {
            this.versionNumber = versionNumber;
        }
        
        public String getVersionNumber() {
            return versionNumber;
        }
        
        public void setHostName(String systemId) {
            this.hostName = systemId;
        }
        
        public String getHostName() {
            return hostName;
        }    
    }
    
    static class ProjectVersionData {

        String projectVersionId;
        List<TableData> projectData;
        List<TableData> resourceData;
        List<TableData> modelData;
        List<TableData> flowData;

        public ProjectVersionData() {
            projectData = new ArrayList<TableData>();
            resourceData = new ArrayList<TableData>();
            modelData = new ArrayList<TableData>();
            flowData = new ArrayList<TableData>();
        }
        
        public String getProjectVersionId() {
            return projectVersionId;
        }

        public void setProjectVersionId(String projectVersionId) {
            this.projectVersionId = projectVersionId;
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

        public List<TableData> getProjectData() {
            return projectData;
        }

        public void setProjectData(List<TableData> projectData) {
            this.projectData = projectData;
        }
        
    }

    static class ImportConfigData extends ConfigData {

        public ImportConfigData(ConfigData configData) {
            this.hostName = configData.getHostName();
            this.versionNumber = configData.getVersionNumber();
            this.releasePackageData = configData.releasePackageData;
            this.projectVersionData = configData.projectVersionData;
            this.deletesToProcess = new HashMap<String, TableData>();
        }
        Map<String, TableData> deletesToProcess;
    }
}
