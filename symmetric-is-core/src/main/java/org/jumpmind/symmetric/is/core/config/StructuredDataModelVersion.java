package org.jumpmind.symmetric.is.core.config;

import java.util.ArrayList;
import java.util.List;

public class StructuredDataModelVersion extends AbstractVersion {

    List<StructuredDataAttribute> attributes = new ArrayList<StructuredDataAttribute>();
    
    public StructuredDataModelVersion add(StructuredDataAttribute attribute) {
        if (this.attributes.contains(attribute)) {
            this.attributes.remove(attribute);
        }
        this.attributes.add(attribute);
        return this;
    }
    
    public StructuredDataModelVersion() {
    }
        
}
