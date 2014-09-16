package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class StructuredModelVersion extends AbstractVersion {

    List<StructuredAttribute> attributes = new ArrayList<StructuredAttribute>();
    
    public StructuredModelVersion add(StructuredAttribute attribute) {
        if (this.attributes.contains(attribute)) {
            this.attributes.remove(attribute);
        }
        this.attributes.add(attribute);
        return this;
    }
    
    public StructuredModelVersion() {
    }
        
}
