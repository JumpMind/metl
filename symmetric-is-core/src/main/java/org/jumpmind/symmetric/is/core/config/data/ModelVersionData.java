package org.jumpmind.symmetric.is.core.config.data;

import java.util.ArrayList;
import java.util.List;

public class ModelVersionData extends AbstractVersionData {

    private static final long serialVersionUID = 1L;
    
    List<ModelAttributeData> attributes = new ArrayList<ModelAttributeData>();
    
    public ModelVersionData add(ModelAttributeData attribute) {
        if (this.attributes.contains(attribute)) {
            this.attributes.remove(attribute);
        }
        this.attributes.add(attribute);
        return this;
    }
    
    public ModelVersionData() {
    }
        
}
