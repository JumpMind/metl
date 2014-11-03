package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.symmetric.app.core.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.config.ComponentFlow;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersionSummary;

public class ConfigurationSqlService extends AbstractConfigurationService {

    String tablePrefix;

    IDatabasePlatform databasePlatform;

    public ConfigurationSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager);
        this.databasePlatform = databasePlatform;
        this.tablePrefix = tablePrefix;
    }

    @Override
    protected String tableName(Class<?> clazz) {
        StringBuilder name = new StringBuilder(tablePrefix);
        String[] tokens = StringUtils.splitByCharacterTypeCamelCase(clazz.getSimpleName()
                .substring(0, clazz.getSimpleName().indexOf("Data")));
        for (String string : tokens) {
            name.append("_");
            name.append(string);
        }
        return name.toString();
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

}
