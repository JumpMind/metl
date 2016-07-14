package org.jumpmind.metl.core.plugin;

import java.util.Map;

import org.jumpmind.metl.core.model.PluginArtifactVersion;

public interface IPluginManager {
    
    public void init();

    public Map<PluginArtifactVersion, ClassLoader> getPlugins();
    
    public void reload();
}
