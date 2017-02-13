package org.jumpmind.metl.core.persist;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.ISqlRowMapper;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.model.ProjectVersionDefinitionPlugin;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.persist.IPersistenceManager;

public class PluginService extends AbstractService implements IPluginService {
    
    IDatabasePlatform databasePlatform;

    public PluginService(ISecurityService securityService, IPersistenceManager persistenceManager, IDatabasePlatform databasePlatform, String tablePrefix) {
        super(securityService, persistenceManager, tablePrefix);
        this.databasePlatform = databasePlatform;
    }
    
    @Override
    public List<Plugin> findPlugins() {
        List<Plugin> plugins = find(Plugin.class, null, Plugin.class);
        Collections.sort(plugins);
        return plugins;
    }

    @Override
    public List<PluginRepository> findPluginRepositories() {
        return find(PluginRepository.class, null, PluginRepository.class);
    }
    
    @Override
    public void refresh(PluginRepository pluginRepository) {
        persistenceManager.refresh(pluginRepository, null, null, tableName(PluginRepository.class));
    }

    
    @Override
    public List<Plugin> findDistinctPlugins() {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.query(String.format("select distinct artifact_group, artifact_name, min(load_order) as load_order from "
                + "%1$s_plugin group by artifact_group, artifact_name", tablePrefix), new ISqlRowMapper<Plugin>() {
                    public Plugin mapRow(Row row) {
                        return new Plugin(row.getString("artifact_group"), row.getString("artifact_name"), row.getInt("load_order"));
                    }
                });
    }

    @Override
    public List<Plugin> findActivePlugins() {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.query(String.format("select distinct artifact_group, artifact_name, load_order from ("
                + "select v.artifact_group, v.artifact_name, p.load_order from %1$s_project_version_definition_plugin v left join "
                + " %1$s_plugin p on v.artifact_name=p.artifact_name and v.artifact_group=p.artifact_group where v.enabled=1) "
                + " order by load_order, artifact_group, artifact_name", tablePrefix), new ISqlRowMapper<Plugin>() {
                    public Plugin mapRow(Row row) {
                        return new Plugin(row.getString("artifact_group"), row.getString("artifact_name"), row.getInt("load_order"));
                    }
                });
    }

    @Override
    public List<Plugin> findUnusedPlugins() {
        ISqlTemplate template = databasePlatform.getSqlTemplate();
        return template.query(String.format(
                "select artifact_group, artifact_name, artifact_version from %1$s_plugin p where not exists "
                        + "(select 1 from %1$s_project_version_definition_plugin v where "
                        + "v.artifact_name=p.artifact_name and v.artifact_group=p.artifact_group and v.artifact_version=p.artifact_version) ",
                tablePrefix), new ISqlRowMapper<Plugin>() {
                    public Plugin mapRow(Row row) {
                        return new Plugin(row.getString("artifact_group"), row.getString("artifact_name"), row.getString("artifact_version"));
                    }
                });
    }
    

    @Override
    public void save(Plugin plugin) {
        plugin.setLastUpdateTime(new Date());
        persistenceManager.save(plugin, null, null, tableName(Plugin.class));
    }

    @Override
    public void delete(Plugin plugin) {
        persistenceManager.delete(plugin, null, null, tableName(Plugin.class));
    }
    
    @Override
    public void delete(ProjectVersionDefinitionPlugin projectVersionDefinitionPlugin) {
        persistenceManager.delete(projectVersionDefinitionPlugin, null, null, tableName(ProjectVersionDefinitionPlugin.class));
    }


    
}
