package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.csv.CsvWriter;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

@ComponentDefinition(typeName = DelimitedFormatter.TYPE, category = ComponentCategory.PROCESSOR, iconImage="format.png",
    inputMessage=MessageType.ENTITY_MESSAGE, 
    outgoingMessage=MessageType.TEXT_MESSAGE)

public class DelimitedFormatter extends AbstractComponent {

    public static final String TYPE = "Delimited Formatter";

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Delimiter", defaultValue=",")
    public final static String DELIMITED_FORMATTER_DELIMITER = "delimited.formatter.delimiter";
    
    @SettingDefinition(order = 20, type = Type.STRING, label = "Quote Character", defaultValue="\"")
    public final static String DELIMITED_FORMATTER_QUOTE_CHARACTER = "delimited.formatter.quote.character";
    
    public final static String DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION = "delimited.formatter.attribute.format.function";
    
    public final static String DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL = "delimited.formatter.attribute.ordinal";
    
    /* settings */
    
    String delimiter;
    
    String quoteCharacter;

    /* other vars */
    
    TypedProperties properties;
    
    List<AttributeFormat> attributes = new ArrayList<AttributeFormat>();    

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        applySettings();
    }

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        
        if (attributes.size() == 0) {
            executionTracker
            .log(executionId, LogLevel.INFO, this,
                    "There are no format attributes configured.  Writing all entity fields to the output.");
        }

        componentStatistics.incrementInboundMessages();
        ArrayList<EntityData> inputRows = inputMessage.getPayload();

        Message outputMessage = new Message(flowStep.getId());
        ArrayList<String> outputPayload = new ArrayList<String>(); 
        
        String outputRec;
        for (EntityData inputRow : inputRows) {
            outputRec = processInputRow(executionId, inputRow);
            outputPayload.add(outputRec);
        } 
        outputMessage.setPayload(outputPayload);
        executionTracker.log(executionId, LogLevel.INFO, this, outputPayload.toString());
        componentStatistics.incrementOutboundMessages();
        outputMessage.getHeader().setSequenceNumber(componentStatistics.getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
    }
    
    private String processInputRow(String executionId, EntityData inputRow) {
   
        Writer writer = new StringWriter();
        CsvWriter csvWriter = new CsvWriter(writer, delimiter.charAt(0));
        if (!StringUtils.isEmpty(quoteCharacter)) {
            csvWriter.setTextQualifier(quoteCharacter.charAt(0));
        }        
        try {
            if (attributes.size() > 0) {
                for (AttributeFormat attribute : attributes) {
                    csvWriter.write(inputRow.get(attribute.getAttributeId()).toString());
                }
            } else {
                Collection<Object> values = inputRow.values();
                for (Object object : values) {
                    csvWriter.write(object != null ? object.toString() : null);
                }

            }
            //csvWriter.endRecord();
        } catch (IOException e) {
            throw new IoException("Error writing to stream for formatted output. " + e.getMessage());
        }
        return writer.toString();
    }
    
    private void applySettings() {
        properties = flowStep.getComponent().toTypedProperties(this, false);
        delimiter = properties.get(DELIMITED_FORMATTER_DELIMITER);
        quoteCharacter = properties.get(DELIMITED_FORMATTER_QUOTE_CHARACTER);
        convertAttributeSettingsToAttributeFormat();
    }
    
    private void convertAttributeSettingsToAttributeFormat() {
        
        List<ComponentAttributeSetting> attributeSettings = flowStep.getComponent().getAttributeSettings();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            if (attributeSetting.getName().equalsIgnoreCase(DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL)) {
                attributes.add(new AttributeFormat(attributeSetting.getAttributeId(), Integer.parseInt(attributeSetting.getValue())));
            }
        }
        Collections.sort(attributes, new Comparator<AttributeFormat>() {
            @Override
            public int compare(AttributeFormat ordinal1, AttributeFormat ordinal2) {
                return ordinal1.getOrdinal() - ordinal2.getOrdinal();
            }
        });
        
    }
    
    private class AttributeFormat {
        
        public AttributeFormat(String attributeId, int ordinal) {
            this.attributeId = attributeId;
            this.ordinal = ordinal;
        }
        
        String attributeId;
        int ordinal;
        
        public String getAttributeId() {
            return attributeId;
        }

        public int getOrdinal() {
            return ordinal;
        }
    }
   
    //TODO: allow for groovy format functions
}
