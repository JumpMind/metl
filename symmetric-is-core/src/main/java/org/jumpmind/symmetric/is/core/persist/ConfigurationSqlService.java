package org.jumpmind.symmetric.is.core.persist;

import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.persist.IPersistenceManager;
import org.jumpmind.symmetric.is.core.model.AgentSummary;
import org.jumpmind.symmetric.is.core.model.Flow;

public class ConfigurationSqlService extends AbstractConfigurationService {

    IDatabasePlatform databasePlatform;

    public ConfigurationSqlService(IDatabasePlatform databasePlatform,
            IPersistenceManager persistenceManager, String tablePrefix) {
        super(persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
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

    @Override
    public List<AgentSummary> findUndeployedAgentsFor(String flowId) {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template
                .query(String
                        .format("select a.name as name, a.host as host, f.name as folder_name, a.id as id from "
                                + "%1$s_agent a inner join "
                                + "%1$s_folder f on f.id=a.folder_id "
                                + "where a.id not in (select agent_id from %1$s_agent_deployment where flow_id = ?)",
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
                }, flowId);
    }
}
