package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;

public class EntityNameLookup {
    
    Map<String, String> cache = new HashMap<>();
    
    Model model;
    
    public EntityNameLookup(Model model) {
        this.model = model;
    }

    public Set<String> getEntityNames(EntityData data) {
        if (model != null) {
            Set<String> names = new HashSet<String>();
            Set<String> attributeIds = data.keySet();
            for (String attributeId : attributeIds) {
                String name = cache.get(attributeId);
                if (isBlank(name)) {
                    ModelAttrib attribute = model.getAttributeById(attributeId);
                    if (attribute != null) {
                        ModelEntity entity = model.getEntityById(attribute.getEntityId());
                        name = entity.getName();
                        cache.put(attributeId, name);
                    }
                }
                names.add(name);
            }
            return names;
        } else {
            return Collections.emptySet();
        }
    }
}
