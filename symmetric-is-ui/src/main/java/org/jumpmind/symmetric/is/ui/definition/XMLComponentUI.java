package org.jumpmind.symmetric.is.ui.definition;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "component-ui")
public class XMLComponentUI implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlAttribute(required = true)
    protected String id;

    @XmlAttribute(required = false)
    protected String componentId;

    @XmlElement
    protected String iconImage;

    @XmlElement
    protected XMLPanels panels;

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setIconImage(String iconImage) {
        this.iconImage = iconImage;
    }

    public String getIconImage() {
        return iconImage;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setPanels(XMLPanels panels) {
        this.panels = panels;
    }

    public XMLPanels getPanels() {
        return panels;
    }

}
