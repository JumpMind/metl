package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.AgentSummary;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersionSummary;

public class ConfigurationSqlService extends AbstractConfigurationService {

    IDatabasePlatform databasePlatform;

    public ConfigurationSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
    }

    @Override
    public boolean isDeployed(ComponentFlow componentFlow) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template
                .queryForInt(
                        String.format(
                                "select count(*) from %1$s_agent_deployment where component_flow_version_id in "
                                        + "(select component_flow_version_id from %1$s_component_flow_version where component_flow_id=?)",
                                tablePrefix), componentFlow.getId()) > 0;
    }

    @Override
    public boolean isDeployed(ComponentFlowVersion componentFlowVersion) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.queryForInt(String.format(
                "select count(*) from %1$s_agent_deployment where component_flow_version_id=?",
                tablePrefix), componentFlowVersion.getId()) > 0;
    }

    @Override
    public List<ComponentFlowVersionSummary> findUndeployedComponentFlowVersionSummary(
            String agentId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template
                .query(String
                        .format("select cf.name as name, cfv.version_name as version_name, f.name as folder_name, cfv.id as id from "
                                + "%1$s_component_flow_version cfv inner join "
                                + "%1$s_component_flow cf on cf.id=cfv.component_flow_id inner join "
                                + "%1$s_folder f on f.id=cf.folder_id "
                                + "where cf.id not in (select component_flow_id from %1$s_component_flow_version where id in "
                                + "(select component_flow_version_id from %1$s_agent_deployment where agent_id=?))",
                                tablePrefix), new ISqlRowMapper<ComponentFlowVersionSummary>() {
                    @Override
                    public ComponentFlowVersionSummary mapRow(Row row) {
                        ComponentFlowVersionSummary summary = new ComponentFlowVersionSummary();
                        summary.setName(row.getString("name"));
                        summary.setId(row.getString("id"));
                        summary.setFolderName(row.getString("folder_name"));
                        summary.setVersionName(row.getString("version_name"));
                        return summary;
                    }
                }, agentId);
    }

    
    @Override
    public List<AgentSummary> findUndeployedAgentsFor(String componentFlowVersionId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template
                .query(String
                        .format("select a.name as name, a.host as host, f.name as folder_name, a.id as id from "
                                + "%1$s_agent a inner join "
                                + "%1$s_folder f on f.id=a.folder_id "
                                + "where a.id not in (select agent_id from %1$s_agent_deployment where component_flow_version_id = ?)",
                                tablePrefix), new ISqlRowMapper<AgentSummary>() {
                    @Override
                    public AgentSummary mapRow(Row row) {
                        AgentSummary summary = new AgentSummary();
                        summary.setName(row.getString("name"));
                        summary.setId(row.getString("id"));
                        summary.setFolderName(row.getString("folder_name"));
                        summary.setHost(row.getString("host"));
                        return summary;
                    }
                }, componentFlowVersionId);
    }
}
