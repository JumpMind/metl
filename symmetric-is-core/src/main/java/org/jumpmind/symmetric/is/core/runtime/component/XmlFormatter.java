package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = XmlFormatter.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "xmlformatter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.TEXT)
public class XmlFormatter extends AbstractComponentRuntime {

    public static final String TYPE = "Format XML";

    public final static String XML_FORMATTER_XPATH = "xml.formatter.xpath";

    public final static String XML_FORMATTER_TEMPLATE = "xml.formatter.template";

    TypedProperties properties;

    Document templateDocument;

    List<XmlFormatterSetting> settings;

    @Override
    protected void start() {
        properties = getComponent().toTypedProperties(getSettingDefinitions(false));
        Setting templateSetting = getComponent().findSetting(XML_FORMATTER_TEMPLATE);

        if (templateSetting != null && StringUtils.isNotBlank(templateSetting.getValue())) {
            SAXBuilder builder = new SAXBuilder();
            builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
            builder.setFeature("http://xml.org/sax/features/validation", false);
            try {
                templateDocument = builder.build(new StringReader(templateSetting.getValue()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        settings = new ArrayList<XmlFormatterSetting>();
        for (ComponentAttributeSetting attrSetting : getComponent().getAttributeSettings()) {
            if (attrSetting.getName().equals(XML_FORMATTER_XPATH)) {
                XPathExpression<Object> expression = XPathFactory.instance().compile(
                        attrSetting.getValue());
                settings.add(new XmlFormatterSetting(attrSetting, expression));
            }
        }
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        ArrayList<EntityData> inputRows = inputMessage.getPayload();

        Message outputMessage = new Message(getFlowStepId());
        ArrayList<String> outputPayload = new ArrayList<String>();

        for (EntityData inputRow : inputRows) {
            outputPayload.add(processInputRow(inputRow));
        }
        outputMessage.setPayload(outputPayload);
        log(LogLevel.INFO, outputPayload.toString());
        getComponentStatistics().incrementOutboundMessages();
        outputMessage.getHeader()
                .setSequenceNumber(getComponentStatistics().getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
    }

    private String processInputRow(EntityData inputRow) {
        Document document = templateDocument.clone();
        
        /*
         * Temporarily remove namespaces
         */
        Map<Element, Namespace> namespaces = new HashMap<Element, Namespace>();
        Namespace rootNameSpace = document.getRootElement().getNamespace();
        document.getRootElement().setNamespace(null);
        for (Element el : document.getRootElement().getDescendants(new ElementFilter())) {
            Namespace nsp = el.getNamespace();
            if (nsp != null) {
                el.setNamespace(null);
                namespaces.put(el, nsp);
            }
        }
        
        for (XmlFormatterSetting setting : settings) {
            String attributeId = setting.getComponentAttributeSetting().getAttributeId();
            if (inputRow.containsKey(attributeId)) {
                Object inputValue = inputRow.get(setting.getComponentAttributeSetting()
                        .getAttributeId());
                String value = (inputValue == null) ? null : inputValue.toString();
                List<Object> matches = setting.getExpression().evaluate(document.getRootElement());
                if (matches.size() == 0) {
                    log(LogLevel.WARN, "XPath expression " + setting.getExpression().getExpression() + " did not find any matches");
                }
                for (Object object : matches) {
                    if (object instanceof Element) {
                        ((Element) object).setText(value);
                    } else if (object instanceof Attribute) {
                        ((Attribute) object).setValue(value);
                    }
                }
            }
        }
        
        /*
         * Add temporarily removed namespaces back
         */
        Set<Element> elements = namespaces.keySet();
        for (Element element : elements) {
            element.setNamespace(namespaces.get(element));
        }
        document.getRootElement().setNamespace(rootNameSpace);
        
        XMLOutputter xmlOutputter = new XMLOutputter();
        xmlOutputter.setFormat(Format.getPrettyFormat());
        return xmlOutputter.outputString(document);
    }

    class XmlFormatterSetting {

        ComponentAttributeSetting componentAttributeSetting;

        XPathExpression<Object> expression;

        XmlFormatterSetting(ComponentAttributeSetting componentAttributeSetting,
                XPathExpression<Object> expression) {
            this.componentAttributeSetting = componentAttributeSetting;
            this.expression = expression;
        }

        public ComponentAttributeSetting getComponentAttributeSetting() {
            return componentAttributeSetting;
        }

        public void setComponentAttributeSetting(ComponentAttributeSetting componentAttributeSetting) {
            this.componentAttributeSetting = componentAttributeSetting;
        }

        public XPathExpression<Object> getExpression() {
            return expression;
        }

        public void setExpression(XPathExpression<Object> expression) {
            this.expression = expression;
        }
    }
}
