package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.jumpmind.symmetric.is.core.model.DataType;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = XsltProcessor.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "xsltprocessor.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.TEXT)
public class XsltProcessor extends AbstractComponentRuntime {

    public static final String TYPE = "XSLT Processor";

    public final static String XSLT_PROCESSOR_STYLESHEET = "xslt.processor.stylesheet";

    private final static String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    Setting stylesheet;
    
    boolean outputAllAttributes;
    
    @Override
    protected void start() {
        stylesheet = getComponent().findSetting(XSLT_PROCESSOR_STYLESHEET);
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();
        ArrayList<EntityData> inputRows = inputMessage.getPayload();

        Message outputMessage = new Message(getFlowStepId());
        ArrayList<String> outputPayload = new ArrayList<String>();
        
        String batchXml = getBatchXml(getComponent().getInputModel(), inputRows, outputAllAttributes);
        System.out.println(batchXml);

        outputMessage.setPayload(outputPayload);
        log(LogLevel.INFO, outputPayload.toString());
        getComponentStatistics().incrementOutboundMessages();
        outputMessage.getHeader().setSequenceNumber(getComponentStatistics().getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
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
                Element recordElement = new Element("record");
                entityElement.addContent(recordElement);

                List<ModelAttribute> attributes = null;
                if (outputAllAttributes) {
                    attributes = entity.getModelAttributes();
                } else {
                    attributes = getModelAttributes(model, entity.getId(), entityData.keySet());
                }
                
                for (ModelAttribute attribute : attributes) {
                    if (attribute != null && attribute.getEntityId().equals(entity.getId())) {
                        Element attributeElement = new Element("attribute");
                        attributeElement.setAttribute("name", attribute.getName());                       
                        Object object = entityData.get(attribute.getId());
                        String value = null;
                        DataType type = attribute.getDataType();
                        
                        if (object != null) {
                            if (type.isTimestamp()) {
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
                ModelAttribute attribute = model.getAttributeById(attributeId);
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

    protected static List<ModelAttribute> getModelAttributes(Model model, String entityId, Set<String> attributeIds) {
        List<ModelAttribute> attributes = new ArrayList<ModelAttribute>();
        for (String attributeId : attributeIds) {
            ModelAttribute attribute = model.getAttributeById(attributeId);
            if (attribute != null && attribute.getEntityId().equals(entityId)) {
                attributes.add(attribute);
            }
        }
        return attributes;
    }
    
}
