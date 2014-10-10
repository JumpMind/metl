package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.ComponentVersion;

public interface IComponentFactory {

    public IComponent create(ComponentVersion componentVersion);

    public void register(Class<? extends IComponent> clazz);

    public List<String> getComponentTypes();

}
