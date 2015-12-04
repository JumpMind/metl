/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.component;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
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
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;

public class XmlParser extends AbstractXMLComponentRuntime {

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
        
        if (entitySettings.size() == 0) {
        	throw new MisconfiguredException("At least one XPATH setting must be provided.");
        }
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            ArrayList<String> inputRows = ((TextMessage)inputMessage).getPayload();
            ArrayList<EntityData> payload = new ArrayList<EntityData>();
            if (inputRows != null) {
                for (String xml : inputRows) {
                    SAXBuilder builder = new SAXBuilder();
                    builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
                    builder.setFeature("http://xml.org/sax/features/validation", false);
                    try {
                        Document document = builder.build(new StringReader(xml));
                        removeNamespaces(document);
                        for (XmlFormatterEntitySetting entitySetting : entitySettings) {
                            List<XmlFormatterAttributeSetting> attributeSettings = entitySetting.getAttributeSettings();
                            List<Element> entityMatches = (List<Element>) entitySetting.getExpression().evaluate(document.getRootElement());
                            for (Element element : entityMatches) {
                                String text = toXML(element);
                                Document childDocument = builder.build(new ByteArrayInputStream(text.getBytes(Charset.forName("utf-8"))));
                                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                                EntityData data = new EntityData();
                                for (XmlFormatterAttributeSetting attributeSetting : attributeSettings) {
                                    boolean resultsFound = false;
                                    Element targetElement = element;
                                    Document targetDocument = childDocument;
                                    do {
                                        List<Object> attributeMatches = (List<Object>) attributeSetting.getExpression()
                                                .evaluate(targetDocument);
                                        for (Object object : attributeMatches) {
                                            resultsFound = true;
                                            if (object instanceof Attribute) {
                                                data.put(attributeSetting.getSetting().getAttributeId(), ((Attribute) object).getValue());
                                            } else if (object instanceof Content) {
                                                data.put(attributeSetting.getSetting().getAttributeId(), ((Content) object).getValue());
                                            } else if (object instanceof Element) {
                                                data.put(attributeSetting.getSetting().getAttributeId(), ((Element) object).getTextTrim());
                                            }
                                        }

                                        if (!resultsFound && !attributeSetting.getExpression().getExpression().startsWith("/" + element.getName()) &&
                                               targetElement.getParentElement() != null) {
                                            targetElement = targetElement.getParentElement();
                                            targetDocument = builder.build(new ByteArrayInputStream(toXML(targetElement).getBytes(Charset.forName("utf-8"))));
                                        } else if (!resultsFound) {
                                            info("Did not find a match for: %s\n in:\n %s", attributeSetting.getExpression().getExpression(), text);
                                            targetDocument = null;
                                            targetElement = null;
                                        }
                                    } while (!resultsFound && targetElement != null);
                                }
                                if (data.size() > 0) {
                                    payload.add(data);
                                } else {
                                    log(LogLevel.WARN,
                                            "Found entity element: <%s/> with no matching attributes.  Please make sure your xpath expressions match",
                                            element.getName());
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }

            callback.sendEntityDataMessage(null, payload);
        }

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
