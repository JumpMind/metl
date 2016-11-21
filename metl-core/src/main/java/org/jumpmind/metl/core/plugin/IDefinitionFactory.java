package org.jumpmind.metl.core.plugin;

import java.util.List;

public interface IDefinitionFactory {

    public void refresh();
    
    public void refresh(String projectVersionId);
    
    XMLComponent getDefinition(String projectVersionId, String id);
    
    List<XMLComponent> getDefinitions(String projectVersionId);
    

}
