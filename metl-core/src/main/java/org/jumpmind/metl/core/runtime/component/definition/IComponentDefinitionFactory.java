package org.jumpmind.metl.core.runtime.component.definition;

import java.util.List;

public interface IComponentDefinitionFactory {

    public void refresh();
    
    public void refresh(String projectVersionId);
    
    XMLComponent getDefinition(String projectVersionId, String id);
    
    List<XMLComponent> getDefinitions(String projectVersionId);
    

}
