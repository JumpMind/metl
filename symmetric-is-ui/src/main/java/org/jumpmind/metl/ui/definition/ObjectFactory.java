package org.jumpmind.symmetric.is.ui.definition;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


@XmlRegistry
public class ObjectFactory {

    private final static QName _UI_QNAME = new QName("", "ui");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jumpmind.symmetric.is.core.model.xml
     * 
     */
    public ObjectFactory() {
    }

    @XmlElementDecl(namespace="", name = "ui")
    public JAXBElement<XMLUI> createConfig(XMLUI value) {
        return new JAXBElement<XMLUI>(_UI_QNAME, XMLUI.class, null, value);
    }

}
