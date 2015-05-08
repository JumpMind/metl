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
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = DelimitedFormatter.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "delimitedformatter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.TEXT)
public class DelimitedFormatter extends AbstractComponent {

    public static final String TYPE = "Format Delimited";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.STRING,
            label = "Delimiter",
            defaultValue = ",")
    public final static String DELIMITED_FORMATTER_DELIMITER = "delimited.formatter.delimiter";

    @SettingDefinition(
            order = 20,
            type = Type.STRING,
            label = "Quote Character",
            defaultValue = "\"")
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
    public void start(IExecutionTracker executionTracker) {
        super.start(executionTracker);
        applySettings();
    }

    @Override
    public void handle( Message inputMessage, IMessageTarget messageTarget) {

        if (attributes.size() == 0) {
            executionTracker
                    .log(LogLevel.INFO, this, "There are no format attributes configured.  Writing all entity fields to the output.");
        }

        componentStatistics.incrementInboundMessages();
        ArrayList<EntityData> inputRows = inputMessage.getPayload();

        Message outputMessage = new Message(flowStep.getId());
        ArrayList<String> outputPayload = new ArrayList<String>();

        String outputRec;
        for (EntityData inputRow : inputRows) {
            outputRec = processInputRow(inputRow);
            outputPayload.add(outputRec);
        }
        outputMessage.setPayload(outputPayload);
        executionTracker.log(LogLevel.INFO, this, outputPayload.toString());
        componentStatistics.incrementOutboundMessages();
        outputMessage.getHeader()
                .setSequenceNumber(componentStatistics.getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
    }

    private String processInputRow(EntityData inputRow) {

        Writer writer = new StringWriter();
        CsvWriter csvWriter = new CsvWriter(writer, delimiter.charAt(0));
        if (!StringUtils.isEmpty(quoteCharacter)) {
            csvWriter.setUseTextQualifier(true);
            csvWriter.setTextQualifier(quoteCharacter.charAt(0));
            csvWriter.setForceQualifier(true);
        }
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

    private void applySettings() {
        properties = flowStep.getComponent().toTypedProperties(getSettingDefinitions(false));
        delimiter = properties.get(DELIMITED_FORMATTER_DELIMITER);
        quoteCharacter = properties.get(DELIMITED_FORMATTER_QUOTE_CHARACTER);
        convertAttributeSettingsToAttributeFormat();
    }

    private void convertAttributeSettingsToAttributeFormat() {
        List<ComponentAttributeSetting> attributeSettings = flowStep.getComponent()
                .getAttributeSettings();
        Map<String, AttributeFormat> formats = new HashMap<String, DelimitedFormatter.AttributeFormat>();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            AttributeFormat format = formats.get(attributeSetting.getAttributeId());
            if (format == null) {
                Model inputModel = flowStep.getComponent().getInputModel();
                ModelAttribute attribute = inputModel.getAttributeById(attributeSetting
                        .getAttributeId());
                ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                format = new AttributeFormat(attributeSetting.getAttributeId(), entity, attribute);
                formats.put(attributeSetting.getAttributeId(), format);
            }
            if (attributeSetting.getName().equalsIgnoreCase(DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL)) {
                format.setOrdinal(Integer.parseInt(attributeSetting.getValue()));
            } else if (attributeSetting.getName().equalsIgnoreCase(
                    DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION)) {
                format.setFormatFunction(attributeSetting.getValue());
            }
        }

        attributes.addAll(formats.values());
        Collections.sort(attributes, new Comparator<AttributeFormat>() {
            @Override
            public int compare(AttributeFormat ordinal1, AttributeFormat ordinal2) {
                return ordinal1.getOrdinal() - ordinal2.getOrdinal();
            }
        });

    }

    private class AttributeFormat {

        public AttributeFormat(String attributeId, ModelEntity entity, ModelAttribute attribute) {
            this.attributeId = attributeId;
        }

        ModelEntity entity;

        ModelAttribute attribute;

        String attributeId;

        int ordinal;

        String formatFunction;

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
