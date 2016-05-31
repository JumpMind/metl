package org.jumpmind.metl.core.runtime.component.definition;

import java.util.List;
import java.util.Map;

public interface IComponentDefinitionFactory {

    XMLComponent getDefinition(String id);

    Map<String, List<String>> getTypesByCategory();

    Map<String, List<XMLComponent>> getDefinitionsByCategory();

}
