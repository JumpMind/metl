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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.jumpmind.properties.TypedProperties;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlParser extends AbstractXMLComponentRuntime {

    public static final String TYPE = "Parse XML";

    List<XmlFormatterEntitySetting> entitySettings;

    Map<String, XmlFormatterEntitySetting> entitySettingsByPath = new HashMap<>();

    boolean optimizeForSpeed = false;

    int rowsPerMessage;

    @Override
    protected void start() {
        super.start();
        TypedProperties properties = getTypedProperties();
        optimizeForSpeed = properties.is("optimize.for.speed");
        rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);

        Model model = getComponent().getOutputModel();
        if (model == null) {
            throw new IllegalStateException("The output model must be defined");
        }
        entitySettings = new ArrayList<XmlParser.XmlFormatterEntitySetting>();

        Component component = getComponent();

        for (ComponentEntitySetting compEntitySetting : component.getEntitySettings()) {
            if (compEntitySetting.getName().equals(XML_FORMATTER_XPATH)) {
                XPathExpression<?> expression = XPathFactory.instance().compile(compEntitySetting.getValue());
                XmlFormatterEntitySetting entitySetting = new XmlFormatterEntitySetting(compEntitySetting, expression);
                entitySettings.add(entitySetting);
                entitySettingsByPath.put(compEntitySetting.getValue(), entitySetting);
                List<ComponentAttributeSetting> attributeSettings = component
                        .getAttributeSettingsFor(entitySetting.getSetting().getEntityId());
                for (ComponentAttributeSetting componentAttributeSetting : attributeSettings) {
                    if (componentAttributeSetting.getName().equals(XML_FORMATTER_XPATH)) {
                        expression = XPathFactory.instance().compile(componentAttributeSetting.getValue());
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
            if (optimizeForSpeed) {
                handleUsingPullParser(inputMessage, callback, unitOfWorkBoundaryReached);
            } else {
                handleUsingXPath(inputMessage, callback, unitOfWorkBoundaryReached);
            }
        }
    }

    protected void handleUsingPullParser(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        try {
            Map<String, String> currentDataAtLevel = new HashMap<>();
            XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
            ArrayList<String> inputRows = ((TextMessage) inputMessage).getPayload();
            ArrayList<EntityData> payload = new ArrayList<EntityData>();
            if (inputRows != null) {
                for (String xml : inputRows) {
                    StringBuilder fullPath = new StringBuilder();
                    List<StringBuilder> shortPaths = new ArrayList<>();
                    parser.setInput(new StringReader(xml));
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        switch (eventType) {
                            case XmlPullParser.START_TAG:
                                fullPath.append("/").append(parser.getName());
                                StringBuilder shortPath = new StringBuilder("/").append(parser.getName());
                                shortPaths.add(shortPath);
                                addAttributes(parser, shortPath, fullPath, currentDataAtLevel);
                                break;
                            case XmlPullParser.END_TAG:
                                EntityData data = processCurrentLevel(fullPath.toString(), currentDataAtLevel);
                                if (data != null) {
                                    payload.add(data);
                                }

                                int index = fullPath.lastIndexOf("/");
                                if (index >= 0) {
                                    fullPath.replace(index, fullPath.length(), "");
                                }
                                break;
                            case XmlPullParser.TEXT:
                                StringBuilder path = shortPaths.get(shortPaths.size()-1);
                                String text = parser.getText();
                                currentDataAtLevel.put(String.format("%s/text()", fullPath), text);
                                currentDataAtLevel.put(fullPath.toString(), text);
                                currentDataAtLevel.put(String.format("%s/text()", path), text);
                                currentDataAtLevel.put(path.toString(), text);

                                break;
                        }

                        if (payload.size() > rowsPerMessage) {
                            callback.sendEntityDataMessage(null, payload);
                            payload = new ArrayList<>();
                        }
                        eventType = parser.next();
                    }
                }

                if (payload.size() > 0) {
                    callback.sendEntityDataMessage(null, payload);
                    payload = new ArrayList<>();
                }

            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void addAttributes(XmlPullParser parser, StringBuilder shortPath, StringBuilder fullPath, Map<String, String> values) {
        int attributeCount = parser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            values.put(String.format("%s/@%s", fullPath, attributeName), attributeValue);
            values.put(String.format("%s/@%s", shortPath, attributeName), attributeValue);
        }
    }

    protected EntityData processCurrentLevel(String path, Map<String, String> currentDataByLevel) {
        EntityData data = null;
        XmlFormatterEntitySetting entitySetting = entitySettingsByPath.get(path);
        if (entitySetting != null) {
            data = new EntityData();
            List<XmlFormatterAttributeSetting> attributeSettings = entitySetting.getAttributeSettings();
            for (XmlFormatterAttributeSetting attributeSetting : attributeSettings) {
                String xpath = attributeSetting.getExpression().getExpression();
                String value = currentDataByLevel.get(xpath);
                if (value != null) {
                    data.put(attributeSetting.getSetting().getAttributeId(), value);
                }
            }
        }
        return data;
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
                            String text = toXML(element);
                            Document childDocument = builder.build(new ByteArrayInputStream(text.getBytes(Charset.forName("utf-8"))));
                            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                            EntityData data = new EntityData();
                            for (XmlFormatterAttributeSetting attributeSetting : attributeSettings) {
                                boolean resultsFound = false;
                                Element targetElement = element;
                                Document targetDocument = childDocument;
                                do {
                                    List<Object> attributeMatches = (List<Object>) attributeSetting.getExpression().evaluate(targetDocument);
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

                                    if (!resultsFound && !attributeSetting.getExpression().getExpression().startsWith("/" + element.getName())
                                            && targetElement.getParentElement() != null) {
                                        targetElement = targetElement.getParentElement();
                                        targetDocument = builder
                                                .build(new ByteArrayInputStream(toXML(targetElement).getBytes(Charset.forName("utf-8"))));
                                    } else if (!resultsFound) {
                                        info("Did not find a match for: %s\n in:\n %s", attributeSetting.getExpression().getExpression(),
                                                text);
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

        ComponentAttributeSetting setting;

        XPathExpression<?> expression;

        XmlFormatterAttributeSetting(ComponentAttributeSetting setting, XPathExpression<?> expression) {
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
