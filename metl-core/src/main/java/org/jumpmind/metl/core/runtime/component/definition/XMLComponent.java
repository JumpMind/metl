package org.jumpmind.metl.core.runtime.component.definition;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "component", propOrder = {})
public class XMLComponent implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlType
    @XmlEnum(String.class)
    public enum MessageType {
        @XmlEnumValue("none")
        NONE(null), @XmlEnumValue("entity")
        ENTITY("E"), @XmlEnumValue("text")
        TEXT("T"), @XmlEnumValue("binary")
        BINARY("B"), @XmlEnumValue("any")
        ANY("*");

        private String letter;

        private MessageType(String letter) {
            this.letter = letter;
        }

        public String getLetter() {
            return letter;
        }
    }

    @XmlType
    @XmlEnum
    public enum ResourceCategory {
        @XmlEnumValue("datasource")
        DATASOURCE, @XmlEnumValue("streamable")
        STREAMABLE, @XmlEnumValue("none")
        NONE, @XmlEnumValue("any")
        ANY
    }

    @XmlElement(required = false)
    protected String description;

    @XmlElement(required = true)
    protected String name;

    @XmlElement(required = true)
    protected String className;

    @XmlAttribute(required = true)
    protected String id;

    @XmlAttribute(required = true)
    protected String category;

    @XmlAttribute(required = false)
    protected boolean shareable;

    @XmlAttribute(required = false)
    protected boolean inputOutputModelsMatch;

    @XmlAttribute(required = false)
    protected MessageType inputMessageType;

    @XmlAttribute(required = false)
    protected MessageType outputMessageType;

    @XmlAttribute(required = false)
    protected ResourceCategory resourceCategory;

    @XmlElement
    protected XMLSettings settings;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public boolean isShareable() {
        return shareable;
    }

    public void setShareable(boolean shareable) {
        this.shareable = shareable;
    }

    public boolean isInputOutputModelsMatch() {
        return inputOutputModelsMatch;
    }

    public void setInputOutputModelsMatch(boolean inputOutputModelsMatch) {
        this.inputOutputModelsMatch = inputOutputModelsMatch;
    }

    public MessageType getInputMessageType() {
        return inputMessageType;
    }

    public void setInputMessageType(MessageType inputMessageType) {
        this.inputMessageType = inputMessageType;
    }

    public MessageType getOutputMessageType() {
        return outputMessageType;
    }

    public void setOutputMessageType(MessageType outputMessageType) {
        this.outputMessageType = outputMessageType;
    }

    public ResourceCategory getResourceCategory() {
        return resourceCategory;
    }

    public void setResourceCategory(ResourceCategory resourceCategory) {
        this.resourceCategory = resourceCategory;
    }

    public void setSettings(XMLSettings settings) {
        this.settings = settings;
    }

    public XMLSettings getSettings() {
        return settings;
    }

}
