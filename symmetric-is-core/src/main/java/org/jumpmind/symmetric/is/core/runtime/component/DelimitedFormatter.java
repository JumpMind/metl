package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.csv.CsvWriter;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = DelimitedFormatter.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "delimitedformatter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.TEXT)
public class DelimitedFormatter extends AbstractComponentRuntime {

    public static final String TYPE = "Format Delimited";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.TEXT,
            label = "Delimiter",
            defaultValue = ",")
    public final static String DELIMITED_FORMATTER_DELIMITER = "delimited.formatter.delimiter";

    @SettingDefinition(
            order = 20,
            type = Type.TEXT,
            label = "Quote Character",
            defaultValue = "\"")
    public final static String DELIMITED_FORMATTER_QUOTE_CHARACTER = "delimited.formatter.quote.character";

    @SettingDefinition(
            order = 30,
            type = Type.BOOLEAN,
            label = "Header line",
            defaultValue = "false")
    public final static String DELIMITED_FORMATTER_WRITE_HEADER = "delimited.formatter.header";

    public final static String DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION = "delimited.formatter.attribute.format.function";

    public final static String DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL = "delimited.formatter.attribute.ordinal";

    /* settings */

    String delimiter;

    String quoteCharacter;

    boolean useHeader;

    /* other vars */

    TypedProperties properties;

    List<AttributeFormat> attributes = new ArrayList<AttributeFormat>();

    @Override
    protected void start() {
        
        applySettings();
    }

    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {

        if (attributes.size() == 0) {
            log(LogLevel.INFO, "There are no format attributes configured.  Writing all entity fields to the output");
        }

        getComponentStatistics().incrementInboundMessages();
        ArrayList<EntityData> inputRows = inputMessage.getPayload();

        Message outputMessage = new Message(getFlowStepId());
        ArrayList<String> outputPayload = new ArrayList<String>();
        
        if (useHeader) {
            Writer writer = new StringWriter();
            CsvWriter csvWriter = getCsvWriter(writer);
            try {
                for (AttributeFormat attr : attributes) {
                    if (attr.getAttribute() != null) {
                        csvWriter.write(attr.getAttribute().getName());
                    }
                }
            } catch (IOException e) {
                throw new IoException("Error writing to stream for formatted output. " + e.getMessage());    
            }
            outputPayload.add(writer.toString());
        }

        String outputRec;
        for (EntityData inputRow : inputRows) {
            outputRec = processInputRow(inputRow);
            outputPayload.add(outputRec);
        }
        outputMessage.setPayload(outputPayload);
        getComponentStatistics().incrementOutboundMessages();
        outputMessage.getHeader().setSequenceNumber(getComponentStatistics().getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
    }

    private String processInputRow(EntityData inputRow) {

        Writer writer = new StringWriter();
        CsvWriter csvWriter = getCsvWriter(writer);
        try {
            if (attributes.size() > 0) {
                for (AttributeFormat attribute : attributes) {
                    Object object = inputRow.get(attribute.getAttributeId());
                    if (isNotBlank(attribute.getFormatFunction())) {
                        object = ModelAttributeScriptHelper.eval(attribute.getAttribute(), object,
                                attribute.getEntity(), inputRow, attribute.getFormatFunction());
                    }

                    csvWriter.write(object != null ? object.toString() : null);
                }
            } else {
                Collection<Object> values = inputRow.values();
                for (Object object : values) {
                    csvWriter.write(object != null ? object.toString() : null);
                }

            }
        } catch (IOException e) {
            throw new IoException("Error writing to stream for formatted output. " + e.getMessage());
        }
        return writer.toString();
    }

    private CsvWriter getCsvWriter(Writer writer) {
        CsvWriter csvWriter = new CsvWriter(writer, delimiter.charAt(0));
        if (!StringUtils.isEmpty(quoteCharacter)) {
            csvWriter.setUseTextQualifier(true);
            csvWriter.setTextQualifier(quoteCharacter.charAt(0));
            csvWriter.setForceQualifier(true);
        }
        return csvWriter;
    }

    private void applySettings() {
        properties = getComponent().toTypedProperties(getSettingDefinitions(false));
        delimiter = properties.get(DELIMITED_FORMATTER_DELIMITER);
        quoteCharacter = properties.get(DELIMITED_FORMATTER_QUOTE_CHARACTER);
        useHeader = properties.is(DELIMITED_FORMATTER_WRITE_HEADER);
        convertAttributeSettingsToAttributeFormat();
    }

    private void convertAttributeSettingsToAttributeFormat() {
        List<ComponentAttributeSetting> attributeSettings = getComponent().getAttributeSettings();
        Map<String, AttributeFormat> formats = new HashMap<String, AttributeFormat>();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            AttributeFormat format = formats.get(attributeSetting.getAttributeId());
            if (format == null) {
                Model inputModel = getComponent().getInputModel();
                ModelAttribute attribute = inputModel.getAttributeById(attributeSetting.getAttributeId());
                ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                format = new AttributeFormat(attributeSetting.getAttributeId(), entity, attribute);
                formats.put(attributeSetting.getAttributeId(), format);
            }
            if (attributeSetting.getName().equalsIgnoreCase(DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL)) {
                format.setOrdinal(Integer.parseInt(attributeSetting.getValue()));
            } else if (attributeSetting.getName().equalsIgnoreCase(DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION)) {
                format.setFormatFunction(attributeSetting.getValue());
            }
        }

        attributes.addAll(formats.values());
        Collections.sort(attributes, new Comparator<AttributeFormat>() {
            public int compare(AttributeFormat ordinal1, AttributeFormat ordinal2) {
                return ordinal1.getOrdinal() - ordinal2.getOrdinal();
            }
        });
    }

    private class AttributeFormat {

        ModelEntity entity;
        ModelAttribute attribute;
        String attributeId;
        int ordinal;
        String formatFunction;

        public AttributeFormat(String attributeId, ModelEntity entity, ModelAttribute attribute) {
            this.attributeId = attributeId;
            this.entity = entity;
            this.attribute = attribute;
        }

        public String getAttributeId() {
            return attributeId;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public void setOrdinal(int ordinal) {
            this.ordinal = ordinal;
        }

        public String getFormatFunction() {
            return formatFunction;
        }

        public void setFormatFunction(String formatFunction) {
            this.formatFunction = formatFunction;
        }

        public ModelAttribute getAttribute() {
            return attribute;
        }

        public ModelEntity getEntity() {
            return entity;
        }
    }

}
