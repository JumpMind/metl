package org.jumpmind.metl.core.runtime.component.definition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "choices", propOrder = {
    "choice"
})
public class XMLSettingChoices implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(name="choice", required = true)
    protected List<String> choice;
    
    public XMLSettingChoices() {
    }
    
    public XMLSettingChoices(String... choices) {
        choice = new ArrayList<>(Arrays.asList(choices));
    }

    public void setChoice(List<String> choice) {
        this.choice = choice;
    }
    
    public List<String> getChoice() {
        return choice;
    }
    
}
