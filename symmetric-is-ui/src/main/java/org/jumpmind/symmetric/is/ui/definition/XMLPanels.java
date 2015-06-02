package org.jumpmind.symmetric.is.ui.definition;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "panels")
public class XMLPanels implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement
    protected List<String> className;
    
    public void setClassName(List<String> clazz) {
        this.className = clazz;
    }
    
    public List<String> getClassName() {
        return className;
    }

}
