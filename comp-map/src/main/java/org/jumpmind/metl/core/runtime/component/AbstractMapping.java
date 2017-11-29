package org.jumpmind.metl.core.runtime.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.ComponentAttribSetting;

public abstract class AbstractMapping extends AbstractComponentRuntime {

    public final static String ATTRIBUTE_MAPS_TO = "mapping.processor.attribute.maps.to";
    public final static String ENTITY_MAPS_TO = "mapping.processor.entity.maps.to";

    protected Map<String, Set<String>> getAttribToAttribMap() {
    		Map<String, Set<String>>attrToAttrMap = new HashMap<String, Set<String>>();
        List<ComponentAttribSetting> attributeSettings = getComponent().getAttributeSettings();
        for (ComponentAttribSetting attributeSetting : attributeSettings) {
            if (attributeSetting.getName().equalsIgnoreCase(ATTRIBUTE_MAPS_TO)) {
                Set<String> targets = attrToAttrMap.get(attributeSetting.getAttributeId());
                if (targets == null) {
                    targets = new HashSet<String>(2);
                    attrToAttrMap.put(attributeSetting.getAttributeId(), targets);
                }
                targets.add(attributeSetting.getValue());
            }
        }
        return attrToAttrMap;
    }  
}
