/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.persist;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.sql.JDBCType;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.tools.ant.Project;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AuditEvent;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.model.Rppv;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.core.security.SecurityConstants;
import org.jumpmind.metl.core.util.MessageException;
import org.jumpmind.metl.core.util.VersionUtils;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.io.data.DbExport;
import org.jumpmind.symmetric.io.data.DbExport.Format;
import org.jumpmind.util.AppUtils;
import org.jumpmind.util.LinkedCaseInsensitiveMap;
import org.springframework.dao.DataIntegrityViolationException;

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
    final static Integer AGENT_IDX = new Integer(0);
    
    final static Integer CREATE_TIME_IDX = new Integer(0);
    final static Integer LAST_UPDATE_TIME_IDX = new Integer(1);
    final static Integer CREATE_BY_IDX = new Integer(2);
    final static Integer LAST_UPDATE_BY_IDX = new Integer(3);

    final String[][] RELEASE_PACKAGE_SQL = {
            {"_release_package","select * from %1$s_release_package where id='%2$s' order by id", "id"},
            {"_rppv","select * from %1$s_rppv where " +
                    "release_package_id='%2$s' order by release_package_id", "release_package_id,project_version_id"}
    };

    final String[][] PROJECT_SQL = {
            {"_project","select * from %1$s_project where id in (select project_id from %1$s_project_version where id='%2$s') union select * from %1$s_project where id='%3$s' order by 1","id"},
            {"_project_version","select * from %1$s_project_version where id='%2$s' order by id","id"},
            {"_project_version_plugin","select * from %1$s_project_version_plugin where project_version_id='%2$s' order by project_version_id","project_version_id,component_type_id"},           
            {"_project_version_depends","select pvd.*, pv.VERSION_LABEL as TARGET_VERSION_LABEL, p.NAME as TARGET_PROJECT_NAME from %1$s_project_version_depends pvd\n" + 
                    "    left join %1$s_project_version pv\n" + 
                    "        on pvd.TARGET_PROJECT_VERSION_ID = pv.ID\n" + 
                    "    left join %1$s_project p\n" + 
                    "        on pv.PROJECT_ID = p.ID\n" + 
                    "where project_version_id='%2$s' order by pv.id","id"}
    };
    
    final String[][] MODEL_SQL = {
            {"_model","select * from %1$s_model where project_version_id='%2$s' and id='%3$s' order by id","id"},
            {"_model_entity","select * from %1$s_model_entity where model_id='%3$s' order by id","id"},
            {"_model_attrib","select * from %1$s_model_attrib where entity_id in "
            + "(select id from %1$s_model_entity where model_id in "
            + "(select id from %1$s_model where project_version_id='%2$s' and id='%3$s')) order by id","id"}
    };
    
    final String[][] RESOURCE_SQL = {
            {"_resource","select * from %1$s_resource where project_version_id = '%2$s' and id='%3$s' order by id","id"},
            {"_resource_setting","select * from %1$s_resource_setting where resource_id='%3$s' order by resource_id, name","resource_id,name"}
    };
    
    final String[][] FLOW_SQL = {
            {"_component","select c.*, r.name as RESOURCE_NAME, p.name as RESOURCE_PROJECT_NAME, pv.version_label as RESOURCE_PROJECT_VERSION \n" + 
                    "from %1$s_component c \n" + 
                    "    left join %1$s_resource r\n" + 
                    "        on c.resource_id = r.id\n" + 
                    "    left join %1$s_project_version pv\n" + 
                    "        on r.project_version_id = pv.id\n" + 
                    "    left join %1$s_project p\n" + 
                    "        on pv.project_id = p.id\n" + 
                    "where c.project_version_id='%2$s' and c.id in \n" + 
                    "    (select distinct component_id from %1$s_flow_step where flow_id='%3$s') order by c.id", "id"},
            {"_component_setting","select * from %1$s_component_setting where component_id in "
                    + "(select distinct component_id from %1$s_flow_step where flow_id='%3$s') order by id", "id"},
            {"_component_entity_setting","select * from %1$s_component_entity_setting where component_id in "
                    + "(select distinct component_id from %1$s_flow_step where flow_id='%3$s') order by id", "id"},
            {"_component_attrib_setting","select * from %1$s_component_attrib_setting where component_id in "
                    + "(select distinct component_id from %1$s_flow_step where flow_id='%3$s') order by id", "id"},
            {"_flow","select * from %1$s_flow where project_version_id='%2$s' and id='%3$s' order by id", "id"},
            {"_flow_parameter","select * from %1$s_flow_parameter where flow_id='%3$s' order by id", "id"},
            {"_flow_step","select * from %1$s_flow_step where flow_id='%3$s' order by id", "id"},
            {"_flow_step_link","select * from %1$s_flow_step_link where source_step_id in "
                    + "(select distinct id from %1$s_flow_step where flow_id='%3$s') order by source_step_id, target_step_id", "source_step_id,target_step_id"}            
    };
    
    final String[][] AGENT_SQL = {
            {"_agent", "select * from %1$s_agent where id='%2$s' and deleted=0 order by id","id"},
            {"_agent_deploy", "select * from %1$s_agent_deploy where agent_id='%2$s' order by id","id"},
            {"_agent_flow_deploy_parm", "select * from %1$s_agent_flow_deploy_parm where agent_deployment_id in (select id from %1$s_agent_deploy where agent_id='%2$s') order by agent_deployment_id, flow_id","agent_deployment_id, flow_id, name"},            
            {"_agent_resource_setting", "select * from %1$s_agent_resource_setting where agent_id='%2$s' order by resource_id, name","agent_id, resource_id, name"},
            {"_agent_parameter", "select * from %1$s_agent_parameter where agent_id='%2$s' order by id","id"},
    };
    
    private IDatabasePlatform databasePlatform;
    private IConfigurationService configurationService;
    private IOperationsService operationsService;
    private String tablePrefix;
    private String[] columnsToExclude;
    private Set<String> importsToAudit = new HashSet<>();
    private Set<String> projectsExported = new HashSet<>();
    private List<IConfigurationChangedListener> configurationChangedListeners = Collections.synchronizedList(new ArrayList<>());

    public ImportExportService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix,
            IConfigurationService configurationService, IOperationsService operationsService,
            ISecurityService securityService) {
        super(securityService, persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
        this.configurationService = configurationService;
        this.operationsService = operationsService;
        this.tablePrefix = tablePrefix;
        importsToAudit.add(tableName(Project.class).toUpperCase());
        importsToAudit.add(tableName(ProjectVersion.class).toUpperCase());
        importsToAudit.add(tableName(Flow.class).toUpperCase());
        importsToAudit.add(tableName(Model.class).toUpperCase());
        importsToAudit.add(tableName(Resource.class).toUpperCase());
        setColumnsToExclude();
    }
    
    @Override
    public void addConfigurationChangeListener(IConfigurationChangedListener listener) {
        configurationChangedListeners.add(listener);
    }

    private void setColumnsToExclude() {
        columnsToExclude = new String[4];
        columnsToExclude[CREATE_TIME_IDX] = "create_time";
        columnsToExclude[LAST_UPDATE_TIME_IDX] = "last_update_time";
        columnsToExclude[CREATE_BY_IDX] = "create_by";
        columnsToExclude[LAST_UPDATE_BY_IDX] = "last_update_by";
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
        initConfigData(exportData.getReleasePackageData(), RELEASE_PACKAGE_SQL);
        ReleasePackage releasePackage = configurationService.findReleasePackage(releasePackageId);
        
        List<Rppv> versions = new ReleasePackageProjectVersionSorter(configurationService).sort(releasePackage);
        for (Rppv releasePackageProjectVersion : versions) {
            String projectVersionId = releasePackageProjectVersion.getProjectVersionId();
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
        
        projectsExported.clear();
        ProjectVersion version = configurationService.findProjectVersion(projectVersionId);
        save(new AuditEvent(AuditEvent.EventType.EXPORT, String.format("%s, flows: %d, models %d, resources: %d", 
                version.getName(), flowIds.size(), modelIds.size(), resourceIds.size()), userId));
        ConfigData exportData = initExport();
        initConfigData(exportData.getReleasePackageData(), RELEASE_PACKAGE_SQL);
        initProjectVersionExport(exportData, projectVersionId);
        addProjectVersionToConfigData(projectVersionId, exportData, new HashSet<String>(flowIds), new HashSet<String>(modelIds), new HashSet<String>(resourceIds));
        
        return serializeExportToJson(exportData);
    }

    protected ConfigData initExport() {
        ConfigData exportData = new ConfigData();
        exportData.setHostName(AppUtils.getHostName());
        exportData.setVersionNumber(VersionUtils.getCurrentVersion());

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
        projectsExported.clear();
        ConfigData configData = deserializeConfigurationData(configDataString);
        convertConfigDataTablesColumnsToLowerCase(configData);
        importConfiguration(configData, userId);
        for (IConfigurationChangedListener l : configurationChangedListeners) {
            l.onMultiRowUpdate();
        }
    }

    private void convertConfigDataTablesColumnsToLowerCase(ConfigData configData) {
        convertTableDataToLowerCase(configData.getReleasePackageData());
        for (ProjectVersionData projectVersionData : configData.getProjectVersionData()) {
            convertTableDataToLowerCase(projectVersionData.getProjectData());
            convertTableDataToLowerCase(projectVersionData.getResourceData());
            convertTableDataToLowerCase(projectVersionData.getModelData());
            convertTableDataToLowerCase(projectVersionData.getFlowData());
        }
        convertTableDataToLowerCase(configData.getAgentData());
    }
    
    private void convertTableDataToLowerCase(List<TableData> tableDatas) {
        for (TableData tableData : tableDatas) {
            tableData.setTableName(tableData.getTableName().toLowerCase());
            Map<String, LinkedCaseInsensitiveMap<Object>> newRows = new HashMap<String, LinkedCaseInsensitiveMap<Object>>();
            tableData.getTableData().forEach((rowId,row)->{
                LinkedCaseInsensitiveMap<Object> newRow = new LinkedCaseInsensitiveMap<Object>();
                row.forEach((colId,value)->{
                    newRow.put(colId.toLowerCase(), value);
                });
                newRows.put(rowId, newRow);
            });
            tableData.setTableData(newRows);
        }
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
        
        // Get existing ProjectVersion
        ProjectVersion version = configurationService.findProjectVersion(projectVersionId);
        for (int i = 0; i < sqlElements.length; i++) {
            if (!sqlElements[0][0].equalsIgnoreCase("_project") ||
                    version == null || !projectsExported.contains(version.getProjectId()) ) {
                String[] entry = sqlElements[i];
                // Query existing data matching the config data.
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
        
        if (version != null) {
            projectsExported.add(version.getProjectId());
        }
    }

    private void addAgentConfigData(List<TableData> tableData, String[][] sqlElements,
            String agentId) {
        for (int i = 0; i <= sqlElements.length - 1; i++) {
            String[] entry = sqlElements[i];
            List<Row> rows = getConfigTableData(String.format(entry[SQL], tablePrefix, agentId));
            for (Row row : rows) {
                if (isPassword(row.getString("NAME", false))) {
                    String value = row.getString("VALUE", false);
                    if (isNotBlank(value)) {
                        if (value.startsWith(SecurityConstants.PREFIX_ENC)) {
                            try {
                                row.put("VALUE", securityService.decrypt(value.substring(SecurityConstants.PREFIX_ENC.length() - 1)));
                            } catch (Exception e) {
                            }
                        }
                    }
                }
                tableData.get(i).rows.put(getPkDataAsString(row, entry[KEY_COLUMNS]), row);
            }
        }
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
        // Copy config data into importData object.
        ImportConfigData importData = new ImportConfigData(configData);
        
        // Get transaction to roll back the import in case of failure.
        ISqlTransaction transaction = databasePlatform.getSqlTemplate().startSqlTransaction();
        try {
            // Load one of many project versions from the imported config.
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
                
                // Delete all local records once the project version has been loaded.
                processDeletes(importData, transaction);                
            }
            if (importData.getReleasePackageData().size() != 0) {
                importReleasePackageConfiguration(importData, transaction, userId);
            }
            if (importData.getAgentData().size() != 0) {
                importAgentConfiguration(importData, transaction, userId);
            }
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
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_flow_step_link"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_flow_step"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_flow_parameter"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_flow"), transaction);
        processTableDeletes(
                importData.deletesToProcess.get(tablePrefix + "_component_attrib_setting"),
                transaction);
        processTableDeletes(
                importData.deletesToProcess.get(tablePrefix + "_component_entity_setting"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_component_setting"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_component"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_resource_setting"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_resource"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_model_attrib"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_model_entity"),
                transaction);
        processTableDeletes(importData.deletesToProcess.get(tablePrefix + "_model"), transaction);
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

    private void importAgentConfiguration(ImportConfigData importData,
            ISqlTransaction transaction, String userId) {

        List<TableData> existingAgentData = new ArrayList<TableData>();
        initConfigData(existingAgentData, AGENT_SQL);        
        
        Iterator<String> itr = importData.getAgentData().get(AGENT_IDX)
                .getTableData().keySet().iterator();
        while (itr.hasNext()) {
            String key = itr.next();
            LinkedCaseInsensitiveMap<Object> row = importData.getAgentData().get(AGENT_IDX).getTableData().get(key);
            addConfigData(existingAgentData, AGENT_SQL, (String) row.get(AGENT_SQL[AGENT_IDX][KEY_COLUMNS]),
                    (String) row.get(AGENT_SQL[AGENT_IDX][KEY_COLUMNS]));
        }
        
        for (int i = 0; i <= AGENT_SQL.length - 1; i++) {
            if (importData.agentData.size() > i) {
                TableData importAgentData = importData.agentData.get(i);
                try {
                    processConfigTableData(importData, existingAgentData.get(i), importAgentData,
                            AGENT_SQL[i][KEY_COLUMNS], transaction, userId);
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
        
        for (int i = 0; i < PROJECT_SQL.length; i++) {
            if (data.projectData.size() > i) {
                TableData importProjectData = data.projectData.get(i);
                try {
                    processConfigTableData(importData, existingProjectData.get(i), importProjectData,
                            PROJECT_SQL[i][KEY_COLUMNS], transaction, userId);
                } catch (RuntimeException e) {
                    if (importProjectData.getTableName().toLowerCase().endsWith("project_version_depends")) {
                        Collection<LinkedCaseInsensitiveMap<Object>> maps = importProjectData.getTableData().values();
                        StringBuilder ids = new StringBuilder();
                        for (LinkedCaseInsensitiveMap<Object> linkedCaseInsensitiveMap : maps) {
                            if (ids.length() > 0) {
                                ids.append(",");
                            }
                            ids.append(linkedCaseInsensitiveMap.get("target_project_name") + "[" + linkedCaseInsensitiveMap.get("target_version_label") + "]");
                        }
                        throw new MessageException(String.format("Missing dependent project.  Please load the following project(s) first: %s",ids)); 
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
            if (isPassword((String)row.get("name"))) {
                String value = (String)row.get("value");
                if (isNotBlank(value)) {
                    if (!value.startsWith(SecurityConstants.PREFIX_ENC)) {
                        row.put("value",
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
                    table.getPrimaryKeyColumns(), table.getColumns(), null, null);
            int[] types = stmt.getTypes();

            Iterator<String> itr = inserts.getTableData().keySet().iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                LinkedCaseInsensitiveMap<Object> row = inserts.getTableData().get(key);
                convertTimestampColumns(table, row);
                Date createTime = new Date();
                row.put("create_time", createTime);
                row.put("last_update_time", createTime);
                useDefaultsForMissingRequiredColumns(table, row);                       
                Object[] values = stmt.getValueArray(row);
                transaction.prepareAndExecute(stmt.getSql(), values, types);
            }
    }

    private void convertTimestampColumns(Table table, LinkedCaseInsensitiveMap<Object> row) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        for (Column column:table.getColumns()) {
            if (column.getMappedType().equalsIgnoreCase(JDBCType.TIMESTAMP.getName()) &&
                    row.get(column.getName()) != null &&
                    ((String)row.get(column.getName())).length()==10) {
                try {
                    Date newDate = format.parse((String) row.get(column.getName()));
                    if (row.get(column.getName()) != null) {
                        row.put(column.getName().toLowerCase(), newDate);   
                    }
                } catch (Exception e) {
                    log.error(String.format("Error converting %s to a valid date", row.get(column.getName())));
                }
            }
        }
    }
    
    private void processTableUpdates(TableData updates, ISqlTransaction transaction) {
            Table table = databasePlatform.getTableFromCache(null, null, updates.getTableName(), false);
            excludeUpdateColumns(table);         
            DmlStatement stmt = databasePlatform.createDmlStatement(DmlType.UPDATE, table.getCatalog(),
                    table.getSchema(), table.getName(), table.getPrimaryKeyColumns(),
                    getUpdateColumns(table), null, null);
            int[] types = stmt.getTypes();
            
            Iterator<String> itr = updates.getTableData().keySet().iterator();
            while (itr.hasNext()) {
                String key = itr.next();
                LinkedCaseInsensitiveMap<Object> row = updates.getTableData().get(key);
                convertTimestampColumns(table, row);
                row.put("last_update_time", new Date());
                useDefaultsForMissingRequiredColumns(table, row);
                try {
                    Object[] values = stmt.getValueArray(row);
                    databasePlatform.getSqlTemplate().update(stmt.getSql(), values, types);                    
                } catch (DataIntegrityViolationException e) {
                    if (updates.getTableName().toLowerCase().endsWith("_component")) {
                        String resourceLocation = "'" + row.get("resource_name")+ "' located in project '" + row.get("resource_project_name") + "' version '" + 
                                row.get("resource_project_version") + "'.";
                        throw new MessageException(String.format("Missing dependent resource.  "
                                + "Please load the following project resource first: %s",resourceLocation)); 
                    } else {
                        throw e;
                    }
                }
            }
    }

    private void useDefaultsForMissingRequiredColumns(Table table,LinkedCaseInsensitiveMap<Object> row) {        
        for (Column column : table.getColumnsAsList()) {
            if (!row.containsKey(column.getName())) {
                row.put(column.getName().toLowerCase(), column.getDefaultValue());
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
                // Move all keys to lower since Spring's named parameter function makes the map case sensitive.
                Map<String, Object> rowLower = new HashMap<String, Object>();
                for (String k : row.keySet()) {
                    rowLower.put(k.toLowerCase(), row.get(k));
                }
                transaction.prepareAndExecute(stmt.getSql().toLowerCase(), rowLower);
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
    
    @Override
    public String exportAgent(String agentId, String userId) {
        Agent agent = operationsService.findAgent(agentId, false);
        save(new AuditEvent(AuditEvent.EventType.EXPORT, String.format("%s", agent.getName()), userId));
        ConfigData exportData = initExport();
        initConfigData(exportData.getAgentData(), AGENT_SQL);
        addAgentConfigData(exportData.getAgentData(), AGENT_SQL, agentId);
        
        return serializeExportToJson(exportData);        
    }
    
    protected String export(String table, String where, String orderBy, Agent agent) throws IOException {
        DbExport export = new DbExport(databasePlatform);
        export.setWhereClause(String.format(where + orderBy, tablePrefix, agent.getId()));
        export.setFormat(Format.SQL);
        export.setUseQuotedIdentifiers(false);
        export.setNoCreateInfo(true);
        return export.exportTables(new String[] { String.format("%s_%s", tablePrefix, table) });
    }

    protected String export(String table, String where, String orderBy, ProjectVersion projectVersion, String[] columnsToExclude)
            throws IOException {
        DbExport export = new DbExport(databasePlatform);
        export.setWhereClause(String.format(where + orderBy, tablePrefix, projectVersion.getId(), projectVersion.getProjectId()));
        export.setFormat(Format.SQL);
        export.setUseQuotedIdentifiers(false);
        export.setNoCreateInfo(true);
        export.setExcludeColumns(columnsToExclude);
        return export.exportTables(new String[] { String.format("%s_%s", tablePrefix, table) });
    }

    protected String export(String table, String where, String orderBy, ProjectVersion projectVersion, Flow flow, String componentIds,
            String[] columnsToExclude) throws IOException {
        DbExport export = new DbExport(databasePlatform);
        export.setWhereClause(String.format(where + orderBy, tablePrefix, projectVersion.getId(), projectVersion.getProjectId(), flow.getId(),
                componentIds));
        export.setFormat(Format.SQL);
        export.setUseQuotedIdentifiers(false);
        export.setNoCreateInfo(true);
        export.setExcludeColumns(columnsToExclude);
        return export.exportTables(new String[] { String.format("%s_%s", tablePrefix, table) });
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
        List<TableData> agentData;

        public ConfigData() {
            releasePackageData = new ArrayList<TableData>();
            projectVersionData = new ArrayList<ProjectVersionData>();
            agentData = new ArrayList<TableData>();
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
        
        public List<TableData> getAgentData() {
            return agentData;
        }

        public void setAgentData(List<TableData> agentData) {
            this.agentData = agentData;
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

    static class AgentData {

        String agentId;
        List<TableData> agentDeployData;
        List<TableData> agentResourceSettingData;
        List<TableData> agentParameterData;

        public AgentData() {
            agentDeployData = new ArrayList<TableData>();
            agentResourceSettingData = new ArrayList<TableData>();
            agentParameterData = new ArrayList<TableData>();
        }

        public String getAgentId() {
            return agentId;
        }

        public void setAgentId(String agentId) {
            this.agentId = agentId;
        }

        public List<TableData> getAgentDeployData() {
            return agentDeployData;
        }

        public void setAgentDeployData(List<TableData> agentDeployData) {
            this.agentDeployData = agentDeployData;
        }

        public List<TableData> getAgentResourceSettingData() {
            return agentResourceSettingData;
        }

        public void setAgentResourceSettingData(List<TableData> agentResourceSettingData) {
            this.agentResourceSettingData = agentResourceSettingData;
        }

        public List<TableData> getAgentParameterData() {
            return agentParameterData;
        }

        public void setAgentParameterData(List<TableData> agentParameterData) {
            this.agentParameterData = agentParameterData;
        }        
    }    
    
    static class ImportConfigData extends ConfigData {

        public ImportConfigData(ConfigData configData) {
            this.hostName = configData.getHostName();
            this.versionNumber = configData.getVersionNumber();
            this.releasePackageData = configData.releasePackageData;
            this.projectVersionData = configData.projectVersionData;
            this.agentData = configData.agentData;
            this.deletesToProcess = new LinkedCaseInsensitiveMap<TableData>();
        }
        Map<String, TableData> deletesToProcess;
    }
}