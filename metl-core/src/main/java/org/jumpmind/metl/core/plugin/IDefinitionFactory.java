package org.jumpmind.metl.core.plugin;

import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;

public interface IDefinitionFactory {

    public void refresh();
    
    public void refresh(String projectVersionId);
    
    public XMLComponentDefinition getComponentDefinition(String projectVersionId, String id);
    
    public List<XMLComponentDefinition> getComponentDefinitions(String projectVersionId);
    
    public XMLResourceDefinition getResourceDefintion(String projectVersionId, String id);
    
    public Set<String> getResourceCategories(String projectVersionId);    
    
    public Set<XMLResourceDefinition> getResourceDefinitions(String projectVersionId, ResourceCategory resourceCategory);    

}
