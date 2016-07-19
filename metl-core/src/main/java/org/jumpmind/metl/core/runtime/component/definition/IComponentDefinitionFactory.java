package org.jumpmind.metl.core.runtime.component.definition;

import java.util.List;
import java.util.Map;

public interface IComponentDefinitionFactory {

    public void init();
    
    XMLComponent getDefinition(String projectVersionId, String id);

    Map<String, List<XMLComponent>> getDefinitionsByCategory(String projectVersionId);

}
