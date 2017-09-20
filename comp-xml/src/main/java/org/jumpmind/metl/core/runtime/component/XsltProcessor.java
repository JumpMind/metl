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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jdom2.transform.XSLTransformer;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class XsltProcessor extends AbstractComponentRuntime {

    public static final String PRETTY_FORMAT = "Pretty";
    
    public static final String COMPACT_FORMAT = "Compact";
    
    public static final String RAW_FORMAT = "Raw";
    
    public static final String OMIT_XML_DECLARATION_FORMAT = "Omit Declaration";

    public final static String OUTPUT_ALL_ATTRIBUTES = "xslt.processor.output.all.attributes";

    public final static String PARAMETER_REPLACEMENT = "xslt.processor.parameter.replacement";

    public final static String XML_FORMAT = "xslt.processor.xml.format";
    
    public final static String OMIT_XML_DECLARATION = "xslt.processor.xml.omit.declaration";

    public static final String TYPE = "XSLT Processor";

    public final static String XSLT_PROCESSOR_STYLESHEET = "xslt.processor.stylesheet";

    private final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    Setting stylesheet;
    
    boolean outputAllAttributes;
    
    boolean useParameterReplacement = true;
    
    boolean omitXmlDeclaration = false;
    
    String xmlFormat;
    
    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        outputAllAttributes = properties.is(OUTPUT_ALL_ATTRIBUTES);
        useParameterReplacement = properties.is(PARAMETER_REPLACEMENT);
        xmlFormat = properties.get(XML_FORMAT);
        omitXmlDeclaration = properties.is(OMIT_XML_DECLARATION, false);
        stylesheet = getComponent().findSetting(XSLT_PROCESSOR_STYLESHEET);
        if (StringUtils.isBlank(stylesheet.getValue())) {
            throw new RuntimeException("The XSLT stylesheet is blank.  Edit the component and set a stylesheet.");
        }
    }
    
    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
            ArrayList<EntityData> inputRows = ((EntityDataMessage)inputMessage).getPayload();

            ArrayList<String> outputPayload = new ArrayList<String>();

            String batchXml = getBatchXml(getComponent().getInputModel(), inputRows, outputAllAttributes);
            String stylesheetXml = stylesheet.getValue();
            if (useParameterReplacement) {
                stylesheetXml = resolveParamsAndHeaders(stylesheetXml, inputMessage);
            }
            String outputXml = getTransformedXml(batchXml, stylesheetXml, xmlFormat, omitXmlDeclaration);
            outputPayload.add(outputXml);

            log(LogLevel.DEBUG, outputPayload.toString());

            callback.sendTextMessage(null, outputPayload);
        }
    }

    public static String getBatchXml(Model model, ArrayList<EntityData> inputRows, boolean outputAllAttributes) {
        SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
        Element root = new Element("batch");
        Document doc = new Document(root);

        for (ModelEntity entity : getModelEntities(model, inputRows)) {
            Element entityElement = new Element("entity");
            entityElement.setAttribute("name", entity.getName());
            root.addContent(entityElement);

            for (EntityData entityData : inputRows) {
                List<ModelAttrib> attributes = null;
                if (outputAllAttributes) {
                    attributes = entity.getModelAttributes();
                } else {
                    attributes = getModelAttributes(model, entity.getId(), entityData.keySet());
                }

                Element recordElement = new Element("record");
                if (attributes.size() > 0) {
                    entityElement.addContent(recordElement);
                }

                for (ModelAttrib attribute : attributes) {
                    if (attribute != null && attribute.getEntityId().equals(entity.getId())) {
                        Element attributeElement = new Element("attribute");
                        attributeElement.setAttribute("name", attribute.getName());                       
                        Object object = entityData.get(attribute.getId());
                        String value = null;
                        DataType type = attribute.getDataType();
                        
                        if (object != null) {
                            if (type.isTimestamp() && object instanceof Date) {
                                value = df.format(object);
                            } else {
                                value = object.toString();
                            }
                        }
                        attributeElement.setAttribute("value", value == null ? "" : value);
                        recordElement.addContent(attributeElement);
                    }
                }
            }
        }
        
        StringWriter writer = new StringWriter();
        XMLOutputter xmlOutput = new XMLOutputter();
        xmlOutput.setFormat(Format.getPrettyFormat());
        try {
            xmlOutput.output(doc, writer);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    protected static List<ModelEntity> getModelEntities(Model model, ArrayList<EntityData> inputRows) {
        Set<ModelEntity> entities = new LinkedHashSet<ModelEntity>();
        for (EntityData entityData : inputRows) {
            for (String attributeId : entityData.keySet()) {
                ModelAttrib attribute = model.getAttributeById(attributeId);
                if (attribute != null) {
                    ModelEntity entity = model.getEntityById(attribute.getEntityId());
                    if (entity != null) {
                        entities.add(entity);
                    }
                }
            }
        }
        return new ArrayList<ModelEntity>(entities);
    }

    protected static List<ModelAttrib> getModelAttributes(Model model, String entityId, Set<String> attributeIds) {
        List<ModelAttrib> attributes = new ArrayList<ModelAttrib>();
        for (String attributeId : attributeIds) {
            ModelAttrib attribute = model.getAttributeById(attributeId);
            if (attribute != null && attribute.getEntityId().equals(entityId)) {
                attributes.add(attribute);
            }
        }
        return attributes;
    }

    public static String getTransformedXml(String inputXml, String stylesheetXml, String xmlFormat, boolean omitXmlDeclaration) {
        StringWriter writer = new StringWriter();
        SAXBuilder builder = new SAXBuilder();
        builder.setXMLReaderFactory(XMLReaders.NONVALIDATING);
        builder.setFeature("http://xml.org/sax/features/validation", false);
        try {
            Document inputDoc = builder.build(new StringReader(inputXml));
            StringReader reader = new StringReader(stylesheetXml);
            XSLTransformer transformer = new XSLTransformer(reader);
            Document outputDoc = transformer.transform(inputDoc);
            XMLOutputter xmlOutput = new XMLOutputter();
            Format format = null;
            if (xmlFormat.equals(COMPACT_FORMAT)) {
                format = Format.getCompactFormat();
            } else if (xmlFormat.equals(RAW_FORMAT)) {
                format = Format.getRawFormat();
            } else {
                format = Format.getPrettyFormat();
            }
            
            format.setOmitDeclaration(omitXmlDeclaration);
            xmlOutput.setFormat(format);
            xmlOutput.output(outputDoc, writer);
            writer.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }
}
