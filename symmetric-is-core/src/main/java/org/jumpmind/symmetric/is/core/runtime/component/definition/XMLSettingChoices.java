package org.jumpmind.symmetric.is.core.runtime.component.definition;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "choices", propOrder = {
    "choice"
})
public class XMLSettingChoices {

    @XmlElement(name="choice", required = true)
    protected List<String> choice;

    public void setChoice(List<String> choice) {
        this.choice = choice;
    }
    
    public List<String> getChoice() {
        return choice;
    }
    
}
