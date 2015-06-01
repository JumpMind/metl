package org.jumpmind.symmetric.is.core.runtime.component.definition;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


@XmlRegistry
public class ObjectFactory {

    private final static QName _Components_QNAME = new QName(null, "components");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: org.jumpmind.symmetric.is.core.model.xml
     * 
     */
    public ObjectFactory() {
    }

    public XMLComponent createXmlComponent() {
        return new XMLComponent();
    }

    @XmlElementDecl(name = "components")
    public JAXBElement<XMLComponents> createConfig(XMLComponents value) {
        return new JAXBElement<XMLComponents>(_Components_QNAME, XMLComponents.class, null, value);
    }

}
