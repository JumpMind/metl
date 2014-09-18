package org.jumpmind.symmetric.is.core.config.data;

import java.util.ArrayList;
import java.util.List;

public class StructuredModelVersionData extends AbstractVersionData {

    List<StructuredAttributeData> attributes = new ArrayList<StructuredAttributeData>();
    
    public StructuredModelVersionData add(StructuredAttributeData attribute) {
        if (this.attributes.contains(attribute)) {
            this.attributes.remove(attribute);
        }
        this.attributes.add(attribute);
        return this;
    }
    
    public StructuredModelVersionData() {
    }
        
}
