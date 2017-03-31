package org.jumpmind.metl.core.plugin;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "resource", propOrder = {})
public class XMLResourceDefinition extends XMLAbstractDefinition {

    private static final long serialVersionUID = 1L;
    
    @XmlAttribute(required = true)
    protected ResourceCategory resourceCategory;

    public XMLResourceDefinition() {
    }
    
    public void setResourceCategory(ResourceCategory resourceCategory) {
        this.resourceCategory = resourceCategory;
    }
    
    public ResourceCategory getResourceCategory() {
        return resourceCategory;
    }
    
    @Override
    public String toString() {
        return getName() + " of category " + getResourceCategory();
    }

}
