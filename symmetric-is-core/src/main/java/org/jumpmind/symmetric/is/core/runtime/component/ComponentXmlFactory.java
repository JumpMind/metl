package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponents;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSettings;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ComponentXmlFactory implements IComponentFactory {

    final Logger log = LoggerFactory.getLogger(getClass());

    Map<String, XMLComponent> componentsById = new HashMap<String, XMLComponent>();

    Map<String, List<String>> componentIdsByCategory = new HashMap<String, List<String>>();

    public ComponentXmlFactory() {
        refresh();
    }

    public void refresh() {
        loadComponentsForClassloader(getClass().getClassLoader());
        // TODO in the future load from other resources
    }

    public IComponentRuntime create(String id) {
        try {
            XMLComponent definition = componentsById.get(id);
            if (definition != null) {
                IComponentRuntime component = (IComponentRuntime) Class.forName(definition.getClassName()).newInstance();
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

    public Map<String, List<String>> getComponentTypes() {
        return componentIdsByCategory;
    }

    public XMLComponent getComonentDefinition(String id) {
        return componentsById.get(id);
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

    protected List<InputStream> loadResources(final String name, final ClassLoader classLoader) {
        try {
            final List<InputStream> list = new ArrayList<InputStream>();
            final Enumeration<URL> systemResources = (classLoader == null ? ClassLoader.getSystemClassLoader() : classLoader)
                    .getResources(name);
            while (systemResources.hasMoreElements()) {
                list.add(systemResources.nextElement().openStream());
            }
            return list;
        } catch (IOException e) {
            throw new IoException(e);
        }
    }
}
