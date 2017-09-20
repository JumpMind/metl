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
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class XPathXmlParser extends AbstractXMLComponentRuntime {

    public static final String TYPE = "XPath XML Parser";

    List<XmlFormatterEntitySetting> entitySettings = new ArrayList<XmlFormatterEntitySetting>();
    
    int rowsPerMessage;

    @Override
    public void start() {
        super.start();
        TypedProperties properties = getTypedProperties();
        rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);

        Model model = getComponent().getOutputModel();
        if (model == null) {
            throw new IllegalStateException("The output model must be defined");
        }
        
        Component component = getComponent();

        for (ComponentEntitySetting compEntitySetting : component.getEntitySettings()) {
            if (compEntitySetting.getName().equals(XML_FORMATTER_XPATH)) {
                String entityXPath = compEntitySetting.getValue();
                XPathExpression<?> expression = XPathFactory.instance().compile(entityXPath);
                XmlFormatterEntitySetting entitySetting = new XmlFormatterEntitySetting(compEntitySetting, expression);
                entitySettings.add(entitySetting);
                
                List<ComponentAttribSetting> attributeSettings = component
                        .getAttributeSettingsFor(entitySetting.getSetting().getEntityId());
                for (ComponentAttribSetting componentAttributeSetting : attributeSettings) {
                    if (componentAttributeSetting.getName().equals(XML_FORMATTER_XPATH)) {
                        String attributeXPath = componentAttributeSetting.getValue();
                        expression = XPathFactory.instance().compile(attributeXPath);
                        entitySetting.getAttributeSettings().add(new XmlFormatterAttributeSetting(componentAttributeSetting, expression));
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

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            handleUsingXPath(inputMessage, callback, unitOfWorkBoundaryReached);
        }
    }
   
    @SuppressWarnings("unchecked")
    protected void handleUsingXPath(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        ArrayList<String> inputRows = ((TextMessage) inputMessage).getPayload();
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
                            
                            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                            EntityData data = new EntityData();
                            for (XmlFormatterAttributeSetting attributeSetting : attributeSettings) {
                                Element targetElement = element;

                                List<Object> attributeMatches = (List<Object>) attributeSetting.getExpression().evaluate(targetElement);
                                
                                for (Object object : attributeMatches) {
                                    if (object instanceof Attribute) {
                                        data.put(attributeSetting.getSetting().getAttributeId(), ((Attribute) object).getValue());
                                    } else if (object instanceof Content) {
                                        data.put(attributeSetting.getSetting().getAttributeId(), ((Content) object).getValue());
                                    } else if (object instanceof Element) {
                                        data.put(attributeSetting.getSetting().getAttributeId(), ((Element) object).getTextTrim());
                                    }
                                }
                                
                                if (attributeMatches.size() == 0) {
                                    info("Did not find a match for: %s\n in:\n %s", attributeSetting.getExpression().getExpression(),
                                            toXML(element));
                                }
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

                    if (payload.size() > rowsPerMessage) {
                        callback.sendEntityDataMessage(null, payload);
                        payload = new ArrayList<>();
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        if (payload.size() > 0) {
            callback.sendEntityDataMessage(null, payload);
        }
    }

    class XmlFormatterAttributeSetting {

        ComponentAttribSetting setting;

        XPathExpression<?> expression;

        XmlFormatterAttributeSetting(ComponentAttribSetting setting, XPathExpression<?> expression) {
            this.setting = setting;
            this.expression = expression;
        }

        public ComponentAttribSetting getSetting() {
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
