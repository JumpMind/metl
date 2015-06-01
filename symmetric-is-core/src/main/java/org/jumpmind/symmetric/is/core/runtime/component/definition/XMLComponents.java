package org.jumpmind.symmetric.is.core.runtime.component.definition;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "components")
public class XMLComponents implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(required = true)
    protected List<XMLComponent> component;
    
    public void setComponent(List<XMLComponent> components) {
        this.component = components;
    }
    
    public List<XMLComponent> getComponent() {
        return component;
    }

}
