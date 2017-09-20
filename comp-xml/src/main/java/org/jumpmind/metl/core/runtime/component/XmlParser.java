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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

public class XmlParser extends AbstractXMLComponentRuntime {

    public static final String TYPE = "Parse XML";

    Map<String, List<XmlFormatterEntitySetting>> entitySettingsByPath = new HashMap<>();  //THIS DOESN'T WORK BECAUSE THERE CAN BE MULTIPLE ENTITY SETTINGS PER PATH
    
    List<XmlFormatterEntitySetting> entitySettings = new ArrayList<XmlFormatterEntitySetting>();

    boolean optimizeForSpeed = false;

    int rowsPerMessage;

    @Override
    public void start() {
        super.start();
        TypedProperties properties = getTypedProperties();
        optimizeForSpeed = properties.is("optimize.for.speed");
        rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);

        Model model = getComponent().getOutputModel();
        if (model == null) {
            throw new IllegalStateException("The output model must be defined");
        }
        
        List<XmlFormatterEntitySetting> entitySettingsForPath;  

        Component component = getComponent();

        for (ComponentEntitySetting compEntitySetting : component.getEntitySettings()) {
            if (compEntitySetting.getName().equals(XML_FORMATTER_XPATH)) {
                String entityXPath = compEntitySetting.getValue();
                XPathExpression<?> expression = XPathFactory.instance().compile(entityXPath);
                XmlFormatterEntitySetting entitySetting = new XmlFormatterEntitySetting(compEntitySetting, expression);
                
                entitySettingsForPath = entitySettingsByPath.get(compEntitySetting.getValue());
                if (entitySettingsForPath == null) {
                    entitySettingsForPath = new ArrayList<XmlParser.XmlFormatterEntitySetting>();
                    entitySettingsByPath.put(compEntitySetting.getValue(), entitySettingsForPath);
                }
                entitySettingsForPath.add(entitySetting);
                entitySettings.add(entitySetting);
                
                List<ComponentAttribSetting> attributeSettings = component
                        .getAttributeSettingsFor(entitySetting.getSetting().getEntityId());
                for (ComponentAttribSetting componentAttributeSetting : attributeSettings) {
                    if (componentAttributeSetting.getName().equals(XML_FORMATTER_XPATH)) {
                        String attributeXPath = componentAttributeSetting.getValue();
                        if (!optimizeForSpeed && attributeXPath.startsWith(entityXPath) && attributeXPath.length() > entityXPath.length()) {
                            attributeXPath = "/*/" + attributeXPath.substring(entityXPath.length()+1);
                        }
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
                    List<StringBuilder> paths = new ArrayList<>();
                    parser.setInput(new StringReader(xml));
                    int eventType = parser.getEventType();
                    while (eventType != XmlPullParser.END_DOCUMENT) {
                        switch (eventType) {
                            case XmlPullParser.START_TAG:
                                String tagName = removeNameSpace(parser.getName());
                                for (StringBuilder path : paths) {
                                    path.append("/").append(tagName);
                                }
                                StringBuilder shortPath = new StringBuilder("/").append(tagName);
                                paths.add(shortPath);
                                addAttributes(parser, paths, currentDataAtLevel);
                                break;
                            case XmlPullParser.END_TAG:
                                List<EntityData> data = processCurrentLevel(paths.get(0).toString(), currentDataAtLevel);
                                if (data != null) {
                                    payload.addAll(data);
                                    getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
                                }

                                String removed = paths.remove(paths.size() - 1).toString();
                                Iterator<Entry<String, String>> entries = currentDataAtLevel.entrySet().iterator();
                                while (entries.hasNext()) {
                                    Entry<String, String> entry = entries.next();
                                    if (entry.getKey().startsWith(removed)) {
                                        entries.remove();
                                    }
                                }

                                for (StringBuilder path : paths) {
                                    int index = path.lastIndexOf("/");
                                    if (index >= 0) {
                                        path.replace(index, path.length(), "");
                                    }
                                }
                                break;
                            case XmlPullParser.TEXT:
                                String text = parser.getText();
                                for (StringBuilder path : paths) {
                                    currentDataAtLevel.put(String.format("%s/text()", path), text);
                                    currentDataAtLevel.put(path.toString(), text);
                                }

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

    protected void addAttributes(XmlPullParser parser, List<StringBuilder> paths, Map<String, String> values) {
        int attributeCount = parser.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            for (StringBuilder path : paths) {
                values.put(String.format("%s/@%s", path, attributeName), attributeValue);
            }
        }
    }

    protected List<EntityData> processCurrentLevel(String path, Map<String, String> currentDataByLevel) {
        List<EntityData> entitiesData = null;
        EntityData data = null;

        if (entitySettingsByPath.get(path) != null) {
            for (XmlFormatterEntitySetting entitySetting : entitySettingsByPath.get(path)) {
                if (entitySetting != null) {                    
                    data = new EntityData();
                    List<XmlFormatterAttributeSetting> attributeSettings = entitySetting
                            .getAttributeSettings();
                    for (XmlFormatterAttributeSetting attributeSetting : attributeSettings) {
                        String xpath = attributeSetting.getExpression().getExpression();
                        String value = currentDataByLevel.get(xpath);
                        if (value != null) {
                            data.put(attributeSetting.getSetting().getAttributeId(), value);
                        }
                    }
                    if (entitiesData == null) {
                        entitiesData = new ArrayList<EntityData>();
                    }
                    entitiesData.add(data);
                }
            }
        }
        return entitiesData;
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
                                        } else {
                                            data.put(attributeSetting.getSetting().getAttributeId(), object);
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
