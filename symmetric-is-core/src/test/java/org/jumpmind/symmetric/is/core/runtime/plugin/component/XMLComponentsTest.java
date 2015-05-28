package org.jumpmind.symmetric.is.core.runtime.plugin.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStreamReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.jumpmind.symmetric.is.core.runtime.plugin.component.XMLComponents;
import org.junit.Test;

public class XMLComponentsTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testReadXml() throws Exception {
        JAXBContext jc = JAXBContext.newInstance(XMLComponents.class.getPackage().getName());
        Unmarshaller unmarshaller = jc.createUnmarshaller();
        InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream("test-components.xml"));
        JAXBElement<XMLComponents> root = (JAXBElement<XMLComponents>) unmarshaller.unmarshal(reader);
        XMLComponents components = root.getValue();
        assertNotNull(components);
        assertEquals("test", components.getComponent().get(0).getName());

    }
}
