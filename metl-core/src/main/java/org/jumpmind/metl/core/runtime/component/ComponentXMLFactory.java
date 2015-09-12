package org.jumpmind.metl.core.runtime.component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponents;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting;
import org.jumpmind.metl.core.runtime.component.definition.XMLSettings;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.metl.core.util.AbstractXMLFactory;

public class ComponentXMLFactory extends AbstractXMLFactory implements IComponentFactory {

    Map<String, XMLComponent> componentsById;

    Map<String, List<String>> componentIdsByCategory;

    synchronized public IComponentRuntime create(String id) {
        try {
            XMLComponent definition = componentsById.get(id);
            if (definition != null) {
                IComponentRuntime component = (IComponentRuntime) Class.forName(definition.getClassName()).newInstance();
                component.register(definition);
                return component;
            } else {
                throw new IllegalStateException("Could not find a class associated with the component id of " + id);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    synchronized public Map<String, List<String>> getComponentTypes() {
        return componentIdsByCategory;
    }

    synchronized public XMLComponent getComonentDefinition(String id) {
        return componentsById.get(id);
    }
    
    @Override
    protected void reset() {
        componentIdsByCategory = new HashMap<String, List<String>>();
        componentsById = new HashMap<String, XMLComponent>();
    }

    @SuppressWarnings("unchecked")
    protected void loadComponentsForClassloader(ClassLoader classLoader) {
        try {
            JAXBContext jc = JAXBContext.newInstance(XMLComponents.class.getPackage().getName());
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            List<InputStream> componentXmls = loadResources("components.xml", classLoader);
            try {
                for (InputStream inputStream : componentXmls) {
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    JAXBElement<XMLComponents> root = (JAXBElement<XMLComponents>) unmarshaller.unmarshal(reader);
                    XMLComponents components = root.getValue();
                    List<XMLComponent> componentList = components.getComponent();
                    for (XMLComponent xmlComponent : componentList) {
                        String id = xmlComponent.getId();
                        if (componentsById.containsKey(id)) {
                            log.warn("There was already a component registered under the id of {}.  Overwriting {} with {}", new Object[] {
                                    id, componentsById.get(id).getClassName(), xmlComponent.getClassName() });
                        }
                        componentsById.put(id, xmlComponent);

                        List<String> ids = componentIdsByCategory.get(xmlComponent.getCategory());
                        if (ids == null) {
                            ids = new ArrayList<String>();
                            componentIdsByCategory.put(xmlComponent.getCategory(), ids);
                        }

                        if (!ids.contains(xmlComponent.getId())) {
                            ids.add(xmlComponent.getId());
                        }

                        if (xmlComponent.getSettings() == null) {
                            xmlComponent.setSettings(new XMLSettings());
                        }

                        if (xmlComponent.getSettings().getSetting() == null) {
                            xmlComponent.getSettings().setSetting(new ArrayList<XMLSetting>());
                        }
                        
                        xmlComponent.getSettings().getSetting().add(0, new XMLSetting(AbstractComponentRuntime.ENABLED, "Enabled", "true", Type.BOOLEAN, true));
                        xmlComponent.getSettings().getSetting().add(new XMLSetting(AbstractComponentRuntime.INBOUND_QUEUE_CAPACITY, "Inbound Queue Capacity", "100", Type.INTEGER, true));
                    }
                }
            } finally {
                for (InputStream inputStream : componentXmls) {
                    IOUtils.closeQuietly(inputStream);
                }

            }
        } catch (Exception e) {
            throw new IoException(e);
        }
    }

}
