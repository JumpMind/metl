package org.jumpmind.symmetric.is.core.runtime.component;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum
public enum MessageType {
        
    @XmlEnumValue("none") NONE(null), 
    @XmlEnumValue("entity") ENTITY("E"), 
    @XmlEnumValue("text") TEXT("T"), 
    @XmlEnumValue("binary") BINARY("B"), 
    @XmlEnumValue("any") ANY("*");
    
    private String letter;
    
    private MessageType(String letter) {
        this.letter = letter;
    }
    
    public String getLetter() {
        return letter;
    }
    
}
