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

import java.io.IOException;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.io.data.DbExport;
import org.jumpmind.symmetric.io.data.DbExport.Format;

public class ConfigurationSqlService extends AbstractConfigurationService {

    IDatabasePlatform databasePlatform;

    public ConfigurationSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
    }
    
    @Override
    public boolean isInstalled() {
        return databasePlatform.getTableFromCache(tableName(Component.class), false) != null;
    }

    @Override
    public boolean isDeployed(Flow flow) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template
                .queryForInt(
                        String.format(
                                "select count(*) from %1$s_agent_deployment where flow_id = ? ",
                                tablePrefix), flow.getId()) > 0;
    }

    public List<AgentDeploymentSummary> findAgentDeploymentSummary(String agentId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.query(String.format(
                "select p.name as project_name, v.version_label, '%2$s' as type, " +
                "d.id, d.name, d.start_type, d.log_level, d.start_expression, d.status " +
                "from %1$s_agent_deployment d " +
                "inner join %1$s_flow f on f.id = d.flow_id " +
                "inner join %1$s_project_version v on v.id = f.project_version_id " +
                "inner join %1$s_project p on p.id = v.project_id " +
                "where d.agent_id = ? " +
                "union " +
                "select distinct p.name, v.version_label, '%3$s', " +
                "r.id, r.name, null, null, null, null " +
                "from %1$s_agent_deployment d " +
                "inner join %1$s_flow f on f.id = d.flow_id " +
                "inner join %1$s_project_version v on v.id = f.project_version_id " +
                "inner join %1$s_project p on p.id = v.project_id " +
                "inner join %1$s_resource r on r.project_version_id = v.id " +
                "where d.agent_id = ? order by 5 ",
                tablePrefix, AgentDeploymentSummary.TYPE_FLOW, AgentDeploymentSummary.TYPE_RESOURCE), 
                new ISqlRowMapper<AgentDeploymentSummary>() {
                    public AgentDeploymentSummary mapRow(Row row) {
                        AgentDeploymentSummary summary = new AgentDeploymentSummary();
                        summary.setProjectName(row.getString("project_name") + " (" + row.getString("version_label") + ")");
                        summary.setType(row.getString("type"));
                        summary.setName(row.getString("name"));
                        summary.setId(row.getString("id"));
                        summary.setStatus(row.getString("status"));
                        summary.setStartType(row.getString("start_type"));
                        summary.setLogLevel(row.getString("log_level"));
                        summary.setStartExpression(row.getString("start_expression"));
                        return summary;
                    }
                }, agentId, agentId);
    }
    
    @Override
    protected List<ModelAttribute> findAllAttributesForModel(String modelId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        String sql = String.format("select * from %1$s_model_attribute where entity_id in (select id from %1$s_model_entity where model_id=?)", tablePrefix);
        return template.query(sql, new ISqlRowMapper<ModelAttribute>() {
            @Override
            public ModelAttribute mapRow(Row row) {
                return persistenceManager.map(row, ModelAttribute.class, null, null, tableName(ModelAttribute.class));
            }
        }, new Object[] {modelId});
    }

    @Override
    public String export(Agent agent) {
        try {
            StringBuilder out = new StringBuilder();
            
            /* @formatter:off */
            String[][] CONFIG = {
                    {"AGENT", "WHERE ID='%2$s' AND DELETED=0"," ORDER BY ID",                                                                                                                                                                              },
                    {"AGENT_DEPLOYMENT", "WHERE AGENT_ID='%2$s'"," ORDER BY ID",                                                                                                                                                                                                                                },
                    {"AGENT_DEPLOYMENT_PARAMETER", "WHERE AGENT_DEPLOYMENT_ID in (SELECT ID FROM %1$s_AGENT_DEPLOYMENT WHERE AGENT_ID='%2$s')"," ORDER BY ID",                                                                                                                                                                                                                         },
                    {"AGENT_PARAMETER", "WHERE AGENT_ID='%2$s'"," ORDER BY ID",                                                                                                                                                                                                            },
                    {"AGENT_RESOURCE_SETTING", "WHERE AGENT_ID='%2$s'"," ORDER BY RESOURCE_ID, NAME",                                                                                                                                                       },
            };
            /* @formatter:on */

            for (int i = CONFIG.length-1; i >= 0; i--) {
                String[] entry = CONFIG[i];
                out.append(String.format("DELETE FROM %s_%s %s;\n", tablePrefix, entry[0], String.format(
                        entry[1],
                        tablePrefix, agent.getId()).replace("AND DELETED=0", "")));
            }

            for (int i = 0; i < CONFIG.length; i++) {
                String[] entry = CONFIG[i];
                out.append(export(entry[0], entry[1], entry[2], agent));
            }
            
            return out.toString();   
        } catch (IOException e) {
            throw new IoException(e);
        }
    }
    
    @Override
    public String export(ProjectVersion projectVersion, Flow flow) {
        
        String componentIds = getComponentIds(flow);
        
        try {        
            StringBuilder out = new StringBuilder();
            
            /* @formatter:off */
            String[][] CONFIG = {
                    {"COMPONENT", "WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0 AND ID IN (%5$s)"," ORDER BY ID",},
                    {"COMPONENT_SETTING", "WHERE COMPONENT_ID IN (%5$s)"," ORDER BY ID",},
                    {"COMPONENT_ENTITY_SETTING", "WHERE COMPONENT_ID IN (%5$s)"," ORDER BY ID",},
                    {"COMPONENT_ATTRIBUTE_SETTING", "WHERE COMPONENT_ID IN (%5$s) AND ATTRIBUTE_ID in (SELECT ID FROM %1$s_MODEL_ATTRIBUTE WHERE ENTITY_ID IN (SELECT ID FROM %1$s_MODEL_ENTITY WHERE MODEL_ID IN (SELECT ID FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0)))"," ORDER BY ID",},
                    {"FLOW", "WHERE ID='%4$s' AND PROJECT_VERSION_ID='%2$s' AND DELETED=0"," ORDER BY ID",},
                    {"FLOW_PARAMETER", "WHERE FLOW_ID = '%4$s'"," ORDER BY ID",},
                    {"FLOW_STEP", "WHERE FLOW_ID = '%4$s'"," ORDER BY ID",},
                    {"FLOW_STEP_LINK", "WHERE SOURCE_STEP_ID IN (SELECT ID FROM %1$s_FLOW_STEP WHERE FLOW_ID = '%4$s')"," ORDER BY SOURCE_STEP_ID, TARGET_STEP_ID",},
            };
            /* @formatter:on */

            String[] columnsToExclude = new String[4];
            columnsToExclude[0] = "CREATE_TIME";
            columnsToExclude[1] = "LAST_UPDATE_TIME";
            columnsToExclude[2] = "CREATE_BY";
            columnsToExclude[3] = "LAST_UPDATE_BY";            
            
            for (int i = CONFIG.length-1; i >= 0; i--) {
                String[] entry = CONFIG[i];
                out.append(String.format("DELETE FROM %s_%s %s;\n", tablePrefix, entry[0], String.format(
                        entry[1],
                        tablePrefix, projectVersion.getId(), projectVersion.getProjectId(),flow.getId(), componentIds)).replace("AND DELETED=0", ""));
            }

            for (int i = 0; i < CONFIG.length; i++) {
                String[] entry = CONFIG[i];
                out.append(export(entry[0], entry[1], entry[2], projectVersion, flow, componentIds, columnsToExclude));
            }
            
            return out.toString();   
        } catch (IOException e) {
            throw new IoException(e);
        }    
    }
    
    protected String getComponentIds(Flow flow) {
        
        StringBuilder componentIds = new StringBuilder();
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        List<Row> results = template.query(String.format("SELECT ID, SHARED FROM %1$s_COMPONENT WHERE ID IN (SELECT COMPONENT_ID FROM %1$s_FLOW_STEP WHERE FLOW_ID='%2$s')",tablePrefix,flow.getId()));
        for (Row row : results) {
            componentIds.append("'");
            componentIds.append(row.get("ID"));
            componentIds.append("'");
            componentIds.append(",");
            if (row.getString("SHARED").equals("1")) {
                throw new UnsupportedOperationException("Cannot export flows that utilize shared components");           
            }
        }
        componentIds.deleteCharAt(componentIds.length()-1);
        return componentIds.toString();
    }
    
    @Override
    public String export(ProjectVersion projectVersion) {
        try {
            StringBuilder out = new StringBuilder();
            
            /* @formatter:off */
            String[][] CONFIG = {
                    {"PROJECT", "WHERE ID='%3$s'"," ORDER BY ID",                                                                                                                                                                              },
                    {"PROJECT_VERSION", "WHERE ID='%2$s'"," ORDER BY ID",                                                                                                                                                                                                                                },
                    {"FOLDER", "WHERE PROJECT_VERSION_ID='%2$s'"," ORDER BY ID",                                                                                                                                                                                                                         },
                    {"MODEL", "WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0"," ORDER BY ID",                                                                                                                                                                                                            },
                    {"MODEL_ENTITY", "WHERE MODEL_ID in (SELECT ID FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0)"," ORDER BY ID",                                                                                                                                                       },
                    {"MODEL_ATTRIBUTE", "WHERE ENTITY_ID IN (SELECT ID FROM %1$s_MODEL_ENTITY WHERE MODEL_ID in (SELECT ID FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0))"," ORDER BY ID",                                                                                              },
                    {"RESOURCE", "WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0"," ORDER BY ID",                                                                                                                                                                                                         },
                    {"RESOURCE_SETTING", "WHERE RESOURCE_ID IN (SELECT ID FROM %1$s_RESOURCE WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0)"," ORDER BY RESOURCE_ID, NAME",                                                                                                                              },
                    {"COMPONENT", "WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0"," ORDER BY ID",                                                                                                                                                                                                        },
                    {"COMPONENT_SETTING", "WHERE COMPONENT_ID IN (SELECT ID FROM %1$s_COMPONENT WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0)"," ORDER BY ID",                                                                                                                                          },
                    {"COMPONENT_ENTITY_SETTING", "WHERE COMPONENT_ID IN (SELECT ID FROM %1$s_COMPONENT WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0)"," ORDER BY ID",                                                                                                                                   },
                    {"COMPONENT_ATTRIBUTE_SETTING", "WHERE COMPONENT_ID IN (SELECT ID FROM %1$s_COMPONENT WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0) AND ATTRIBUTE_ID in (SELECT ID FROM %1$s_MODEL_ATTRIBUTE WHERE ENTITY_ID IN (SELECT ID FROM %1$s_MODEL_ENTITY WHERE MODEL_ID IN (SELECT ID FROM %1$s_MODEL WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0)))"," ORDER BY ID",                                      },
                    {"FLOW", "WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0"," ORDER BY ID",                                                                                                                                                                                                             },
                    {"FLOW_PARAMETER", "WHERE FLOW_ID IN (SELECT ID FROM %1$s_FLOW WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0)"," ORDER BY ID",                                                                                                                                                       },
                    {"FLOW_STEP", "WHERE FLOW_ID IN (SELECT ID FROM %1$s_FLOW WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0)"," ORDER BY ID",                                                                                                                                                            },
                    {"FLOW_STEP_LINK", "WHERE SOURCE_STEP_ID IN (SELECT ID FROM %1$s_FLOW_STEP WHERE FLOW_ID IN (SELECT ID FROM %1$s_FLOW WHERE PROJECT_VERSION_ID='%2$s' AND DELETED=0))"," ORDER BY SOURCE_STEP_ID, TARGET_STEP_ID",                                                                   },
            };
            /* @formatter:on */

            String[] columnsToExclude = new String[4];
            columnsToExclude[0] = "CREATE_TIME";
            columnsToExclude[1] = "LAST_UPDATE_TIME";
            columnsToExclude[2] = "CREATE_BY";
            columnsToExclude[3] = "LAST_UPDATE_BY";            
            
            for (int i = CONFIG.length-1; i >= 0; i--) {
                String[] entry = CONFIG[i];
                out.append(String.format("DELETE FROM %s_%s %s;\n", tablePrefix, entry[0], String.format(
                        entry[1],
                        tablePrefix, projectVersion.getId(), projectVersion.getProjectId())).replace("AND DELETED=0", ""));
            }

            for (int i = 0; i < CONFIG.length; i++) {
                String[] entry = CONFIG[i];
                out.append(export(entry[0], entry[1], entry[2], projectVersion, columnsToExclude));
            }
            
            return out.toString();   
        } catch (IOException e) {
            throw new IoException(e);
        }
    }
    
    protected String export (String table, String where, String orderBy, Agent agent) throws IOException {
        DbExport export = new DbExport(databasePlatform);        
        export.setWhereClause(String.format(
                where + orderBy,
                tablePrefix, agent.getId()));
        export.setFormat(Format.SQL);
        export.setUseQuotedIdentifiers(false);
        export.setNoCreateInfo(true);
        return export.exportTables(new String[] { String
                .format("%s_%s", tablePrefix, table) });
    }
    
    protected String export (String table, String where, String orderBy, ProjectVersion projectVersion, String[] columnsToExclude) throws IOException {
        DbExport export = new DbExport(databasePlatform);        
        export.setWhereClause(String.format(
                where + orderBy,
                tablePrefix, projectVersion.getId(), projectVersion.getProjectId()));
        export.setFormat(Format.SQL);
        export.setUseQuotedIdentifiers(false);
        export.setNoCreateInfo(true);
        export.setExcludeColumns(columnsToExclude);
        return export.exportTables(new String[] { String
                .format("%s_%s", tablePrefix, table) });
    }
 
    protected String export (String table, String where, String orderBy, ProjectVersion projectVersion, Flow flow, String componentIds, String[] columnsToExclude) throws IOException {
        DbExport export = new DbExport(databasePlatform);        
        export.setWhereClause(String.format(
                where + orderBy,
                tablePrefix, projectVersion.getId(), projectVersion.getProjectId(), flow.getId(), componentIds));
        export.setFormat(Format.SQL);
        export.setUseQuotedIdentifiers(false);
        export.setNoCreateInfo(true);
        export.setExcludeColumns(columnsToExclude);
        return export.exportTables(new String[] { String
                .format("%s_%s", tablePrefix, table) });
    }    
    
    @Override
    public boolean isUserLoginEnabled() {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.queryForInt(String.format("select count(*) from %1$s_user where password is not null", tablePrefix)) > 0;
    }

}
