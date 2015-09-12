package org.jumpmind.metl.core.runtime.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.output.XMLOutputter;
import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.properties.TypedProperties;

abstract public class AbstractXML extends AbstractComponentRuntime {

    @SettingDefinition(
            order = 10,
            required = false,
            type = Type.BOOLEAN,
            label = "Ignore namespaces for XPath matching",
            defaultValue = "true")
    public final static String IGNORE_NAMESPACE = "xml.formatter.ignore.namespace";

    public final static String XML_FORMATTER_XPATH = "xml.formatter.xpath";

    boolean ignoreNamespace = true;
    
    protected String toXML(Element element) {
        XMLOutputter xmlOutputter = new XMLOutputter();
        return xmlOutputter.outputString(element);
    }

    @Override
    protected void start() {
        TypedProperties properties = getTypedProperties();
        ignoreNamespace = properties.is(IGNORE_NAMESPACE);

    }

    protected Map<Element, Namespace> removeNamespaces(Document document) {
        Map<Element, Namespace> namespaces = new HashMap<Element, Namespace>();
        if (ignoreNamespace) {
            namespaces.put(document.getRootElement(), document.getRootElement().getNamespace());
            document.getRootElement().setNamespace(null);
            for (Element el : document.getRootElement().getDescendants(new ElementFilter())) {
                Namespace nsp = el.getNamespace();
                if (nsp != null) {
                    el.setNamespace(null);
                    namespaces.put(el, nsp);
                }
            }
        }
        return namespaces;
    }

    protected void restoreNamespaces(Document document, Map<Element, Namespace> namespaces) {
        if (ignoreNamespace) {
            Set<Element> elements = namespaces.keySet();
            for (Element element : elements) {
                element.setNamespace(namespaces.get(element));
            }
        }
    }

}
