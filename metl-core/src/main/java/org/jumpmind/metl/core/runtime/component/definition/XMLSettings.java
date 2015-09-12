package org.jumpmind.metl.core.runtime.component.definition;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "settings", propOrder = {
    "setting"
})
public class XMLSettings implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(required = true)
    protected List<XMLSetting> setting;
    
    public void setSetting(List<XMLSetting> setting) {
        this.setting = setting;
    }
    
    public List<XMLSetting> getSetting() {
        return setting;
    }
    
}
