package org.jumpmind.symmetric.is.ui.definition;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ui")
public class XMLUI implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement
    protected List<XMLComponentUI> componentUis;
    
    public void setComponentUis(List<XMLComponentUI> componentUis) {
        this.componentUis = componentUis;
    }
    
    public List<XMLComponentUI> getComponentUis() {
        return componentUis;
    }

}
