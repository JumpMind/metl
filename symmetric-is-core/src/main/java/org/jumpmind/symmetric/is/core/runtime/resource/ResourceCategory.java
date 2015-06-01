package org.jumpmind.symmetric.is.core.runtime.resource;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

@XmlType
@XmlEnum
public enum ResourceCategory {

    @XmlEnumValue("datasource") DATASOURCE, 
    @XmlEnumValue("streamable") STREAMABLE, 
    @XmlEnumValue("none") NONE, 
    @XmlEnumValue("any") ANY
    
}
