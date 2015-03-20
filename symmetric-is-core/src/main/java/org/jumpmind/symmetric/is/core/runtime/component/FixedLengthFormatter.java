package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.csv.CsvWriter;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

@ComponentDefinition(typeName = FixedLengthFormatter.TYPE, category = ComponentCategory.PROCESSOR, iconImage = "format.png", 
  inputMessage = MessageType.ENTITY_MESSAGE, outgoingMessage = MessageType.TEXT_MESSAGE)
public class FixedLengthFormatter extends AbstractComponent {

    public static final String TYPE = "Fixed Length Formatter";

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Quote Character")
    public final static String FIXED_LENGTH_FORMATTER_QUOTE_CHARACTER = "fixed.length.formatter.quote.character";

    @SettingDefinition(order = 20, required = true, type = Type.INTEGER, label = "Ordinal")
    public final static String FIXED_LENGTH_FORMATTER_ATTRIBUTE_ORDINAL = "fixed.length.formatter.attribute.ordinal";

    @SettingDefinition(order = 30, required = true, type = Type.INTEGER, label = "Ordinal")
    public final static String FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH = "fixed.length.formatter.attribute.length";

    /* settings */
    String delimiter;
    String quoteCharacter;

    /* other vars */
    TypedProperties properties;
    List<AttributeFormat> attributesList;
    StringBuilder stringBuilder;

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        applySettings();
    }

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {

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
        messageTarget.put(outputMessage);
    }

    private String processInputRow(EntityData inputRow) {

        Writer writer = new StringWriter();
        CsvWriter csvWriter = new CsvWriter(writer, delimiter.charAt(0));
        if (!StringUtils.isEmpty(quoteCharacter)) {
            csvWriter.setTextQualifier(quoteCharacter.charAt(0));
        }
        // try {
        // for (AttributeFormat format : attributesList) {
        // //stringBuilder.append(b)
        // //csvWriter.write(inputRow.get(attribute.getAttributeId()).toString());
        // }
        // //csvWriter.endRecord();
        // } catch (IOException e) {
        // throw new
        // IoException("Error writing to stream for formatted output. " +
        // e.getMessage());
        // }
        return writer.toString();
    }

    private void applySettings() {
        properties = flowStep.getComponent().toTypedProperties(this, false);
        quoteCharacter = properties.get(FIXED_LENGTH_FORMATTER_QUOTE_CHARACTER);
        convertAttributeSettingsToAttributeFormat();
    }

    private void convertAttributeSettingsToAttributeFormat() {

        Map<String, AttributeFormat> attributesMap = new HashMap<String, AttributeFormat>();

        List<ComponentAttributeSetting> attributeSettings = flowStep.getComponent()
                .getAttributeSettings();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            if (attributeSetting.getName().equalsIgnoreCase(
                    FIXED_LENGTH_FORMATTER_ATTRIBUTE_ORDINAL)) {
                if (attributesMap.containsKey(attributeSetting.getAttributeId())) {
                    attributesMap.get(attributeSetting.getAttributeId()).setOrdinal(
                            Integer.parseInt(attributeSetting.getValue()));
                } else {
                    attributesMap.put(attributeSetting.getAttributeId(), new AttributeFormat(
                            Integer.parseInt(attributeSetting.getValue()), -1));
                }
            } else if (attributeSetting.getName().equalsIgnoreCase(
                    FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH)) {
                if (attributesMap.containsKey(attributeSetting.getAttributeId())) {
                    attributesMap.get(attributeSetting.getAttributeId()).setLength(
                            Integer.parseInt(attributeSetting.getValue()));
                } else {
                    attributesMap.put(attributeSetting.getAttributeId(), new AttributeFormat(-1,
                            Integer.parseInt(attributeSetting.getValue())));
                }
            }
        }

        attributesList = new ArrayList<AttributeFormat>(attributesMap.values());

        Collections.sort(attributesList, new Comparator<AttributeFormat>() {
            @Override
            public int compare(AttributeFormat format1, AttributeFormat format2) {
                return format1.getOrdinal() - format2.getOrdinal();
            }
        });
    }

    private class AttributeFormat {

        int ordinal;
        int length;

        public AttributeFormat(int ordinal, int length) {
            this.ordinal = ordinal;
            this.length = length;
        }

        public int getOrdinal() {
            return ordinal;
        }

        public void setOrdinal(int ordinal) {
            this.ordinal = ordinal;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }
    }

    // TODO: allow for groovy format functions
}
