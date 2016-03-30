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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.springframework.util.StringUtils;

public class XmlFormatter extends AbstractXMLComponentRuntime {

    public static final String PRETTY_FORMAT = "Pretty";

    public static final String COMPACT_FORMAT = "Compact";

    public static final String RAW_FORMAT = "Raw";

    public final static String XML_FORMAT = "xml.formatter.xml.format";
    
    public static final String NULL_HANDLING_XML_NIL = "XML nil";
    
    public static final String NULL_HANDLING_EMPTY = "Empty Element";
    
    public static final String NULL_HANDLING_REMOVE = "Remove Element";
    
    public static final String NULL_HANDLING = "xml.formatter.null.handling";

    public static final String TYPE = "Format XML";

    public final static String XML_FORMATTER_TEMPLATE = "xml.formatter.template";

    boolean ignoreNamespace = true;

    String xmlFormat;

    String template;
    
    String nullHandling;
    
    ArrayList<Message> messagesToProcess;

    Map<String, DocElement> entityAttributeDtls;

    Document templateDoc;         
    
    Model inputModel;

    @Override
    protected void start() {
        super.start();
        TypedProperties properties = getTypedProperties();
        ignoreNamespace = properties.is(IGNORE_NAMESPACE);
        xmlFormat = properties.get(XML_FORMAT);
        template = properties.get(XML_FORMATTER_TEMPLATE);
        nullHandling = properties.get(NULL_HANDLING);
        messagesToProcess = new ArrayList<Message>();
        inputModel = getComponent().getInputModel(); 
        templateDoc = getTemplateDoc();                 
        entityAttributeDtls = fillEntityAttributeDetails(templateDoc);
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        
        if (inputMessage instanceof ControlMessage) {
            createXml(callback);
        } else if (inputMessage instanceof EntityDataMessage) {
            messagesToProcess.add(inputMessage);
        } else {
            //todo log error, throw exception
        }
    }

    private void createXml(ISendMessageCallback callback) {

        Document generatedXml = new Document();
        Stack<DocElement> parentStack = new Stack<DocElement>();
        ArrayList<String> outboundPayload = new ArrayList<String>();
        
        for (Message msg : messagesToProcess) {
            processMsgEntities(parentStack, msg, generatedXml);            
        }
        XMLOutputter xmlOutputter = new XMLOutputter();
        Format format = null;
        if (xmlFormat.equals(COMPACT_FORMAT)) {
            format = Format.getCompactFormat();
        } else if (xmlFormat.equals(RAW_FORMAT)) {
            format = Format.getRawFormat();
        } else {
            format = Format.getPrettyFormat();
        }
        xmlOutputter.setFormat(format);
        outboundPayload.add(xmlOutputter.outputString(generatedXml));
        callback.sendTextMessage(null, outboundPayload);
    }
    
    private void processMsgEntities(Stack<DocElement> parentStack, Message msg, Document generatedXml) {
        
        ArrayList<EntityData> inputRows = ((EntityDataMessage)msg).getPayload();
        for (EntityData inputRow : inputRows) {
            Iterator<Entry<String, Object>> itr = inputRow.entrySet().iterator();
            String lastEntityId="";
            while (itr.hasNext()) {
                Entry<String, Object> attribute = itr.next();
                String entityId = inputModel.getAttributeById(attribute.getKey()).getEntityId();
                if (entityId != null && !entityId.equalsIgnoreCase(lastEntityId)) {
                    addXmlElement(parentStack, entityId, "", generatedXml);
                    lastEntityId = entityId;
                }
                addXmlElement(parentStack, attribute.getKey(), attribute.getValue(), generatedXml);
            }
        }
    }

    private void addXmlElement(Stack<DocElement> parentStack, String key, Object attrValue, Document generatedXml) {

        //get parent we should attach to
        DocElement templateDocElement = entityAttributeDtls.get(key);
        DocElement parentToAttach = null;
        DocElement newDocElement = null;
        String value = attrValue == null ? null : attrValue.toString();

        if (templateDocElement != null && 
                (!StringUtils.isEmpty(value) || !nullHandling.equals(NULL_HANDLING_REMOVE))) {
            
            if (parentStack.isEmpty()) {
                fillStackWithStaticParentElements(parentStack, templateDocElement, generatedXml);
            }  
            
            while (!parentStack.isEmpty() && templateDocElement.level <= parentStack.peek().level) {
                parentToAttach = parentStack.pop();
            }
            
            //create the new doc element
            if (templateDocElement.xmlElement != null) {
                
                Element newElement = templateDocElement.xmlElement.clone();
                newElement.removeContent();
                removeAllAttributes(newElement);
                
                if (StringUtils.isEmpty(value)) {
                    if (nullHandling.equalsIgnoreCase(NULL_HANDLING_XML_NIL)) {
                        newElement.setAttribute("nil", "true", getXmlNamespace());
                    }
                } else {
                    newElement.setText(value);
                }
                
                parentToAttach.xmlElement.addContent(newElement);
                newDocElement = new DocElement(parentToAttach.level+1,newElement,null);
            } else {
                Attribute newAttribute = templateDocElement.xmlAttribute.clone();
                if (value != null) {
                    newAttribute.setValue(value);
                }
                parentToAttach.xmlElement.setAttribute(newAttribute);
                newDocElement = new DocElement(parentToAttach.level+1,null,newAttribute);
            }      
            parentStack.push(parentToAttach);
            parentStack.push(newDocElement);
        }
    }

    private final static Namespace getXmlNamespace() {
        return Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }

    private void fillStackWithStaticParentElements(Stack<DocElement> parentStack, DocElement firstDocElement, 
            Document generatedXml) {

        Element newRootElement = templateDoc.getRootElement().clone();
        generatedXml.setRootElement(newRootElement);
        Element elementToPutOnStack = newRootElement;
        int level=0;
        for (level=0;level<firstDocElement.level;level++) {
            List<Element> childElement = elementToPutOnStack.getChildren();
            elementToPutOnStack = childElement.get(0);
        }
        elementToPutOnStack.removeContent();
        removeAllAttributes(elementToPutOnStack);
        parentStack.push(new DocElement(level, elementToPutOnStack,null));
    }
    
    private void removeAllAttributes(Element element) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.addAll(element.getAttributes());
        for (Attribute attribute : attributes) {
            element.removeAttribute(attribute);
        }
    }
        
    private Map<String, DocElement> fillEntityAttributeDetails(Document templateDoc) {
        
        Map<String, DocElement> entityAttributeLevels = new HashMap<String, DocElement>();  
        entityAttributeLevels.putAll(fillEntityDetails(templateDoc));
        entityAttributeLevels.putAll(fillAttributeDetails(templateDoc));
        return entityAttributeLevels;
    }
    
    private Map<String, DocElement> fillAttributeDetails(Document templateDoc) {

        Map<String, DocElement> attributeLevels = new HashMap<String, DocElement>();
        Map<Element, Namespace> namespaces = removeNamespaces(templateDoc);
        for (ComponentAttributeSetting compAttributeSetting : getComponent()
                .getAttributeSettings()) {
            if (compAttributeSetting.getName().equals(XML_FORMATTER_XPATH)) {
                XPathExpression<Object> expression = XPathFactory.instance()
                        .compile(compAttributeSetting.getValue());
                List<Object> matches = expression.evaluate(templateDoc.getRootElement());
                if (matches.size() == 0) {
                    log(LogLevel.WARN, "XPath expression " + compAttributeSetting.getValue()
                            + " did not find any matches");
                } else {
                    if (matches.get(0) instanceof Element) {
                        Element element = (Element) matches.get(0);
                        // a model attribute could never be the root element of
                        // the doc
                        int level = 0;
                        Element elementToMatch = element.getParentElement();
                        while (!elementToMatch.getName()
                                .equalsIgnoreCase(templateDoc.getRootElement().getName())) {
                            elementToMatch = elementToMatch.getParentElement();
                            level++;
                        }
                        attributeLevels.put(compAttributeSetting.getAttributeId(),
                                new DocElement(level, element, null));
                    }
                    if (matches.get(0) instanceof Attribute) {
                        Attribute attribute = (Attribute) matches.get(0);
                        int level = 0;
                        Element elementToMatch = attribute.getParent();
                        while (!elementToMatch.getName()
                                .equalsIgnoreCase(templateDoc.getRootElement().getName())) {
                            elementToMatch = elementToMatch.getParentElement();
                            level++;
                        }
                        attributeLevels.put(compAttributeSetting.getAttributeId(),
                                new DocElement(level, null, attribute));
                    }
                }
            }
        }
        restoreNamespaces(templateDoc, namespaces);
        return attributeLevels;
    }

    private Map<String, DocElement> fillEntityDetails(Document templateDoc) {

        Map<String, DocElement> entityLevels = new HashMap<String, DocElement>();

        Map<Element, Namespace> namespaces = removeNamespaces(templateDoc);
        for (ComponentEntitySetting compEntitySetting : getComponent().getEntitySettings()) {
            if (compEntitySetting.getName().equals(XML_FORMATTER_XPATH)) {
                XPathExpression<Element> expression = XPathFactory.instance()
                        .compile(compEntitySetting.getValue(), Filters.element());
                List<Element> matches = expression.evaluate(templateDoc.getRootElement());
                if (matches.size() == 0) {
                    log(LogLevel.WARN, "XPath expression " + compEntitySetting.getValue()
                            + " did not find any matches");
                } else {
                    int level = 0;
                    Element element = matches.get(0);
                    if (!element.isRootElement()) {
                        Element elementToMatch = element.getParentElement();
                        while (!elementToMatch.getName()
                                .equalsIgnoreCase(templateDoc.getRootElement().getName())) {
                            elementToMatch = elementToMatch.getParentElement();
                            level++;
                        }
                    }
                    entityLevels.put(compEntitySetting.getEntityId(),
                            new DocElement(level, element, null));
                }
            }
        }
        restoreNamespaces(templateDoc, namespaces);
        return entityLevels;
    }
    
    private Document getTemplateDoc() {
        
        Document templateDoc=null;
        
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        try {
            templateDoc = builder.build(new StringReader(resolveFlowParams(template)));
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        return templateDoc;
    }
    
    class DocElement {

        int level;
        Element xmlElement;
        Attribute xmlAttribute;
        
        public DocElement(int level, Element xmlElement, Attribute xmlAttribute) {
            this.level = level;
            this.xmlElement = xmlElement;
            this.xmlAttribute = xmlAttribute;
        }
    }
}
