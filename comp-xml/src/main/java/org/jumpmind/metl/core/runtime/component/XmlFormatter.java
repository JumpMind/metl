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

import org.apache.commons.lang.StringUtils;
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
import org.jdom2.xpath.XPathHelper;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

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

    String runWhen;

    @Override
    public void start() {
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
        runWhen = getComponent().get(RUN_WHEN, PER_MESSAGE);
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {

        if (!(inputMessage instanceof ControlMessage)) {
            messagesToProcess.add(inputMessage);
        }

        if ((PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage)
                || (!PER_UNIT_OF_WORK.equals(runWhen)
                        && !(inputMessage instanceof ControlMessage))) {
            createXml(callback);
            messagesToProcess.clear();
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

    private void processMsgEntities(Stack<DocElement> parentStack, Message msg,
            Document generatedXml) {

        boolean entityUsed = false;
        
        ArrayList<EntityData> inputRows = ((EntityDataMessage) msg).getPayload();
        // Every entity row in the message
        String lastEntityId = "";
        for (EntityData inputRow : inputRows) {
            Iterator<Entry<String, Object>> itr = inputRow.entrySet().iterator();
            while (itr.hasNext()) {
                Entry<String, Object> attribute = itr.next();
                String entityId = inputModel.getAttributeById(attribute.getKey()).getEntityId();
                // deal with creation of a new entity record within the xml
                if (entityId != null && !entityId.equalsIgnoreCase(lastEntityId)) {
                    entityUsed = addModelEntityXml(parentStack, generatedXml, entityId);
                    if (entityUsed) {
                        lastEntityId = entityId;
                    }
                }
                addModelAttributeXml(parentStack, attribute.getKey(), attribute.getValue(),
                        generatedXml, entityId);
            }
            lastEntityId = null;
            getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
        }
    }

    private boolean addModelEntityXml(Stack<DocElement> parentStack, Document generatedXml,
            String entityId) {

        DocElement entityDocElement = entityAttributeDtls.get(entityId);
        boolean entityUsed = false;

        if (entityDocElement != null) {
            entityUsed = true;
            while (!parentStack.isEmpty() && parentStack.peek().level >= entityDocElement.level) {
                parentStack.pop();
            }
            Element entityElementToAdd = entityDocElement.xmlElement.clone();            
            if (parentStack.isEmpty() || parentStack.peek().level < entityDocElement.level - 1) {
                //if the entity level isn't the root
                if (!entityElementToAdd.getName().equalsIgnoreCase(templateDoc.getRootElement().getName())) {
                    fillStackWithStaticParentElements(parentStack, entityDocElement, generatedXml);
                }
            }
            //if the entity level is the root
            if (entityElementToAdd.getName().equalsIgnoreCase(templateDoc.getRootElement().getName())) {
                generatedXml.setRootElement(entityElementToAdd);
            } else {            
                DocElement parentToAttach = parentStack.peek();
                parentToAttach.xmlElement.addContent(0,entityElementToAdd);
            }
            parentStack.push(new DocElement(entityDocElement.level, entityElementToAdd, null,
                    entityDocElement.xpath));
        }
        return entityUsed;
    }

    private void fillStackWithStaticParentElements(Stack<DocElement> parentStack,
            DocElement firstDocElement, Document generatedXml) {

        Element elementToPutOnStack = null;
        Map<Element, Namespace> namespaces = null;

        // if the generatedXml doc is empty then start a new one and use it for
        // search
        if (!generatedXml.hasRootElement()) {
            Element newRootElement = templateDoc.getRootElement().clone();
            generatedXml.setRootElement(newRootElement);
            namespaces = removeNamespaces(generatedXml);
            XPathExpression<Element> expression = XPathFactory.instance()
                    .compile(firstDocElement.xpath, Filters.element());
            List<Element> matches = expression.evaluate(generatedXml.getRootElement());
            if (matches.size() != 0 && matches.get(0).getParentElement() != null) {
                elementToPutOnStack = matches.get(0).getParentElement();
            } else {
                elementToPutOnStack = generatedXml.getRootElement();
            }
            elementToPutOnStack.removeContent();
            removeAllEmptyAttributes(elementToPutOnStack);
            parentStack.push(
                    new DocElement(firstDocElement.level - 1, elementToPutOnStack, null, null));
            restoreNamespaces(generatedXml, namespaces);
        } else {
            // we already have a genertedXml going, but need other static
            // elements from the template
            namespaces = removeNamespaces(templateDoc);
            XPathExpression<Element> expression = XPathFactory.instance()
                    .compile(firstDocElement.xpath, Filters.element());
            List<Element> matches = expression.evaluate(templateDoc.getRootElement());
            // TODO: do something here for when the attribute is more than one
            // level away from the entity
            if (matches.size() != 0) {
                elementToPutOnStack = matches.get(0).getParentElement().clone();
            } else {
                // throw some exception here
            }
            elementToPutOnStack.removeContent();
            removeAllEmptyAttributes(elementToPutOnStack);
            parentStack.push(
                    new DocElement(firstDocElement.level - 1, elementToPutOnStack, null, null));
            restoreNamespaces(templateDoc, namespaces);
        }
    }

    private void addModelAttributeXml(Stack<DocElement> parentStack, String attributeId,
            Object modelAttrValue, Document generatedXml, String entityId) {

        DocElement templateDocElement = entityAttributeDtls.get(attributeId);
        String value = modelAttrValue == null ? null : modelAttrValue.toString();
        Element newElement = null;
        Element templateParentElement = null;
        String templateParentXPath = null;
        Attribute newAttribute = null;
        Map<Element, Namespace> generatedDocNamespaces = null;
        Map<Element, Namespace> templateNamespaces = null;
        Stack<Element> parentsToAdd = new Stack<Element>();
        DocElement entityDocElement = entityAttributeDtls.get(entityId);

        generatedDocNamespaces = removeNamespaces(generatedXml);
        templateNamespaces = removeNamespaces(templateDoc);

        // we can be passed elements in the model that don't reside in the
        // template. If so, just ignore the field and do nothing
        if (templateDocElement != null) {
            // at this point, our stack should always currently have the entity
            // for this attribute as the top level of the stack

            // set up our new element or attribute to add
            if (templateDocElement.xmlElement != null) {
                // we have to add an element
                newElement = templateDocElement.xmlElement.clone();
                newElement.removeContent();
                removeAllEmptyAttributes(newElement);

                if (StringUtils.isEmpty(value)) {
                    if (nullHandling.equalsIgnoreCase(NULL_HANDLING_XML_NIL)) {
                        newElement.setAttribute("nil", "true", getXmlNamespace());
                    }
                } else {
                    newElement.setText(value);
                }
            } else {
                // we have to add an attribute
                newAttribute = templateDocElement.xmlAttribute.clone();
                if (value != null) {
                    newAttribute.setValue(value);
                }
            }

            // in this case the attribute is one lower than the entity and
            // should simply be attached to the entity
            if (templateDocElement.level - 1 == parentStack.peek().level) {
                if (newElement != null) {
                    applyAttributeXPath(generatedXml, templateDocElement.xpath, value);
                } else {
                    parentStack.peek().xmlElement.setAttribute(newAttribute);
                }
            } else {
                // the attribute doesn't hang directly off the entity
                // we must find its parent in the existing doc or fill static
                // content as appropriate

                // first get the parent element for this model attribute, and
                // gets its xpath
                XPathExpression<Element> expression = XPathFactory.instance()
                        .compile(templateDocElement.xpath, Filters.element());
                List<Element> matches = expression.evaluate(templateDoc.getRootElement());
                if (matches.size() != 0) {
                    templateParentElement = matches.get(0).getParentElement();
                } else {
                    // throw an exception, we should always find the element in
                    // the template
                }

                // now look for parent elements in the generated xml until we
                // find one
                // or we hit the entity itself
                boolean parentFound = false;
                do {
                    templateParentXPath = XPathHelper.getRelativePath(entityDocElement.xmlElement,
                            templateParentElement);
                    expression = XPathFactory.instance().compile(templateParentXPath,
                            Filters.element());
                    matches = expression.evaluate(parentStack.peek().xmlElement);
                    if (matches.size() == 0) {
                        Element elementToAdd = templateParentElement.clone();
                        elementToAdd.removeContent();
                        removeAllEmptyAttributes(elementToAdd);
                        parentsToAdd.push(elementToAdd);
                        templateParentElement = templateParentElement.getParentElement();
                    } else {
                        parentFound = true;
                    }
                } while (parentFound == false);

                // add every parent we couldn't find up to the entity level
                Element elementToAddTo = matches.get(0);
                while (!parentsToAdd.isEmpty()) {
                    elementToAddTo.addContent(0,parentsToAdd.peek());
                    elementToAddTo = parentsToAdd.pop();
                }

                // add our model attribute to the latest level
                if (newElement != null) {
                    applyAttributeXPath(generatedXml, templateDocElement.xpath, value);
                } else {
                    elementToAddTo.setAttribute(newAttribute);
                }
            }
        }
        restoreNamespaces(templateDoc, templateNamespaces);
        restoreNamespaces(generatedXml, generatedDocNamespaces);
    }

    private void applyAttributeXPath(Document generatedXml, String xpath, String value) {
        List<Object> matches = XPathFactory.instance().compile(xpath).evaluate(generatedXml.getRootElement());
        if (matches.size() == 0) {
            log(LogLevel.WARN, "XPath expression " + xpath + " did not find any matches");
            return;
        }
        Object object = matches.get(0);
        if (object instanceof Element) {
            Element element = (Element) object;
            if (value != null) {
                element.setText(value.toString());
            } else {
                if (nullHandling.equals(NULL_HANDLING_REMOVE)) {
                    Element parent = element.getParentElement();
                    parent.removeContent(element);
                } else if (nullHandling.equalsIgnoreCase(NULL_HANDLING_XML_NIL)) {
                    element.setAttribute("nil", "true", getXmlNamespace());
                    // Remove template example content
                    element.removeContent();
                } else if (nullHandling.equalsIgnoreCase(NULL_HANDLING_EMPTY)) {
                    // Remove template example content
                    element.removeContent();
                }
            }
        } else if (object instanceof Attribute) {
            Attribute attribute = (Attribute) object;
            if (value != null) {
                attribute.setValue(value);
            }
        }
    }

    private final static Namespace getXmlNamespace() {
        return Namespace.getNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
    }

    private void removeAllEmptyAttributes(Element element) {
        List<Attribute> attributes = new ArrayList<Attribute>();
        attributes.addAll(element.getAttributes());
        for (Attribute attribute : attributes) {
            if (StringUtils.isEmpty(attribute.getValue())) {
                element.removeAttribute(attribute);
            }
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
        for (ComponentAttribSetting compAttributeSetting : getComponent()
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
                        int level = 1;
                        Element elementToMatch = element.getParentElement();
                        while (!elementToMatch.getName()
                                .equalsIgnoreCase(templateDoc.getRootElement().getName())) {
                            elementToMatch = elementToMatch.getParentElement();
                            level++;
                        }
                        attributeLevels.put(compAttributeSetting.getAttributeId(), new DocElement(
                                level, element, null, compAttributeSetting.getValue()));
                    }
                    if (matches.get(0) instanceof Attribute) {
                        Attribute attribute = (Attribute) matches.get(0);
                        int level = 1;
                        Element elementToMatch = attribute.getParent();
                        while (!elementToMatch.getName()
                                .equalsIgnoreCase(templateDoc.getRootElement().getName())) {
                            elementToMatch = elementToMatch.getParentElement();
                            level++;
                        }
                        attributeLevels.put(compAttributeSetting.getAttributeId(), new DocElement(
                                level, null, attribute, compAttributeSetting.getValue()));
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
                    int level = 1;
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
                            new DocElement(level, element, null, compEntitySetting.getValue()));
                }
            }
        }
        restoreNamespaces(templateDoc, namespaces);
        return entityLevels;
    }

    private Document getTemplateDoc() {

        Document templateDoc = null;

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
        String xpath;

        public DocElement(int level, Element xmlElement, Attribute xmlAttribute, String xpath) {
            this.level = level;
            this.xmlElement = xmlElement;
            this.xmlAttribute = xmlAttribute;
            this.xpath = xpath;
        }
    }
}
