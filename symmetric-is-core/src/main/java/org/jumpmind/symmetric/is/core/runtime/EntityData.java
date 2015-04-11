package org.jumpmind.symmetric.is.core.runtime;

import org.jumpmind.symmetric.is.core.util.NameValue;
import org.jumpmind.util.LinkedCaseInsensitiveMap;

public class EntityData extends LinkedCaseInsensitiveMap<Object> {

    private static final long serialVersionUID = 1L;
    
    public EntityData() {
    }
    
    public EntityData(NameValue...nameValues) {
        if (nameValues != null) {
            for (NameValue nameValue : nameValues) {
                put(nameValue.getName(), nameValue.getValue());
            }
        }
    }


}
