package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Attribute;
import org.jdom2.Content;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.ComponentEntitySetting;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = XmlParser.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "xmlformatter.png",
        inputMessage = MessageType.TEXT,
        outgoingMessage = MessageType.ENTITY)
public class XmlParser extends AbstractXML {

    public static final String TYPE = "Parse XML";

    List<XmlFormatterEntitySetting> entitySettings;

    @Override
    protected void start() {
        super.start();
        Model model = getComponent().getOutputModel();
        if (model == null) {
            throw new IllegalStateException("The output model must be defined");
        }
        entitySettings = new ArrayList<XmlParser.XmlFormatterEntitySetting>();

        Component component = getComponent();

        for (ComponentEntitySetting compEntitySetting : component.getEntitySettings()) {
            if (compEntitySetting.getName().equals(XML_FORMATTER_XPATH)) {
                XPathExpression<?> expression = XPathFactory.instance().compile(
                        compEntitySetting.getValue());
                XmlFormatterEntitySetting entitySetting = new XmlFormatterEntitySetting(
                        compEntitySetting, expression);
                entitySettings.add(entitySetting);
                List<ComponentAttributeSetting> attributeSettings = component
                        .getAttributeSettingsFor(entitySetting.getSetting().getEntityId());
                for (ComponentAttributeSetting componentAttributeSetting : attributeSettings) {
                    if (componentAttributeSetting.getName().equals(XML_FORMATTER_XPATH)) {
                        expression = XPathFactory.instance().compile(
                                componentAttributeSetting.getValue());
                        entitySetting.getAttributeSettings().add(
                                new XmlFormatterAttributeSetting(componentAttributeSetting,
                                        expression));
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        ArrayList<String> inputRows = inputMessage.getPayload();
        ArrayList<EntityData> payload = new ArrayList<EntityData>();
        for (String xml : inputRows) {
            SAXBuilder builder = new SAXBuilder();
            builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
            builder.setFeature("http://xml.org/sax/features/validation", false);
            try {
                Document document = builder.build(new StringReader(xml));
                removeNamespaces(document);
                for (XmlFormatterEntitySetting entitySetting : entitySettings) {
                    List<XmlFormatterAttributeSetting> attributeSettings = entitySetting
                            .getAttributeSettings();
                    List<Element> entityMatches = (List<Element>) entitySetting.getExpression()
                            .evaluate(document.getRootElement());
                    for (Element element : entityMatches) {
                        String text = toXML(element);
                        Document childDocument = builder.build(new ByteArrayInputStream(text.getBytes()));
                        getComponentStatistics().incrementNumberEntitiesProcessed();
                        EntityData data = new EntityData();
                        for (XmlFormatterAttributeSetting attributeSetting : attributeSettings) {
                            List<Object> attributeMatches = (List<Object>) attributeSetting
                                    .getExpression().evaluate(childDocument);
                            for (Object object : attributeMatches) {
                                if (object instanceof Attribute) {
                                    data.put(attributeSetting.getSetting().getAttributeId(),
                                            ((Attribute) object).getValue());
                                } else if (object instanceof Content) {
                                    data.put(attributeSetting.getSetting().getAttributeId(),
                                            ((Content) object).getValue());
                                } else if (object instanceof Element) {
                                    data.put(attributeSetting.getSetting().getAttributeId(),
                                            ((Element) object).getTextTrim());
                                }
                            }
                        }
                        if (data.size() > 0) {
                            payload.add(data);
                        } else {
                            log(LogLevel.WARN, "Found entity element: <%s/> with no attributes",
                                    element.getName());
                        }
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        Message msg = new Message(getFlowStepId());
        msg.getHeader().setSequenceNumber(inputMessage.getHeader().getSequenceNumber());
        msg.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        msg.setPayload(payload);
        messageTarget.put(msg);

    }

    class XmlFormatterAttributeSetting {

        ComponentAttributeSetting setting;

        XPathExpression<?> expression;

        XmlFormatterAttributeSetting(ComponentAttributeSetting setting,
                XPathExpression<?> expression) {
            this.setting = setting;
            this.expression = expression;
        }

        public ComponentAttributeSetting getSetting() {
            return setting;
        }

        public XPathExpression<?> getExpression() {
            return expression;
        }
    }

    class XmlFormatterEntitySetting {

        ComponentEntitySetting setting;

        XPathExpression<?> expression;

        List<XmlFormatterAttributeSetting> attributeSettings;

        XmlFormatterEntitySetting(ComponentEntitySetting setting, XPathExpression<?> expression) {
            this.setting = setting;
            this.expression = expression;
            this.attributeSettings = new ArrayList<XmlFormatterAttributeSetting>();
        }

        public ComponentEntitySetting getSetting() {
            return setting;
        }

        public XPathExpression<?> getExpression() {
            return expression;
        }

        public List<XmlFormatterAttributeSetting> getAttributeSettings() {
            return attributeSettings;
        }

    }

}
