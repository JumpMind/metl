package org.jumpmind.metl.core.runtime.component;

import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;

public interface IComponentFactory {

    public IComponentRuntime create(String type);
    
    public Map<String, List<String>> getComponentTypes();

    public XMLComponent getComonentDefinition(String id);
    
    public void refresh();

}
