package org.jumpmind.symmetric.is.core.runtime.plugin.component;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "component", propOrder = {
})
public class XMLComponent {

    @XmlAttribute(name = "name")
    protected String name;

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
