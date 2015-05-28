package org.jumpmind.symmetric.is.core.runtime.plugin.component;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "components", propOrder = {
    "component"
})
public class XMLComponents {

    @XmlElement(required = true)
    protected List<XMLComponent> component;
    
    public void setComponent(List<XMLComponent> components) {
        this.component = components;
    }
    
    public List<XMLComponent> getComponent() {
        return component;
    }

}
