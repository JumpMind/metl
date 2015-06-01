package org.jumpmind.symmetric.is.core.runtime.component.definition;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "setting")
public class XMLSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlType
    @XmlEnum
    public enum Type {
        @XmlEnumValue("text")TEXT, 
        @XmlEnumValue("integer")INTEGER, 
        @XmlEnumValue("boolean")BOOLEAN, 
        @XmlEnumValue("choice")CHOICE, 
        @XmlEnumValue("password")PASSWORD, 
        @XmlEnumValue("xml")XML, 
        @XmlEnumValue("multiline_text")MULTILINE_TEXT, 
        @XmlEnumValue("script")SCRIPT, 
        @XmlEnumValue("source step")SOURCE_STEP
    };
    
    @XmlElement(required = false)
    protected String description;

    @XmlElement(required = true)
    protected String name;
    
    @XmlElement(required = false)
    protected String defaultValue;

    @XmlAttribute(required = true)
    protected String id;
    
    @XmlAttribute(required = true)
    protected Type type;
    
    @XmlAttribute(required = false)
    protected boolean required = false;
    
    @XmlElement(required = false)
    protected XMLSettingChoices choices;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public XMLSettingChoices getChoices() {
        return choices;
    }

    public void setChoices(XMLSettingChoices choices) {
        this.choices = choices;
    }
    
    
    
    


}
