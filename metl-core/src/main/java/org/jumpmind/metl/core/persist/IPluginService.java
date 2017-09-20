package org.jumpmind.metl.core.persist;

import java.util.List;

import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.core.model.ProjectVersionPlugin;

public interface IPluginService {

    public List<PluginRepository> findPluginRepositories();
    
    public List<Plugin> findDistinctPlugins();

    public List<Plugin> findActivePlugins();

    public List<Plugin> findUnusedPlugins();

    public List<Plugin> findPlugins();
    
    public void refresh(PluginRepository pluginRepository);
    
    public void save(Plugin plugin);

    public void delete(Plugin plugin);
    
    public void delete(ProjectVersionPlugin plugin);

}
