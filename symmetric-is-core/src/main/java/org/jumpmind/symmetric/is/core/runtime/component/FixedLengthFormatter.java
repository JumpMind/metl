package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

@ComponentDefinition(typeName = FixedLengthFormatter.TYPE, category = ComponentCategory.PROCESSOR, iconImage = "format.png", inputMessage = MessageType.ENTITY_MESSAGE, outgoingMessage = MessageType.TEXT_MESSAGE)
public class FixedLengthFormatter extends AbstractComponent {

    public static final String TYPE = "Fixed Length Formatter";

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Quote Character")
    public final static String FIXED_LENGTH_FORMATTER_QUOTE_CHARACTER = "fixed.length.formatter.quote.character";

    public final static String FIXED_LENGTH_FORMATTER_ATTRIBUTE_ORDINAL = "fixed.length.formatter.attribute.ordinal";
    public final static String FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH = "fixed.length.formatter.attribute.length";
    public final static String FIXED_LENGTH_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION = "fixed.length.formatter.attribute.format.function";

    /* settings */
    String delimiter;
    String quoteCharacter;

    /* other vars */
    TypedProperties properties;
    List<AttributeFormat> attributesList;

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        applySettings();
    }

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        if (attributesList == null || attributesList.size() == 0) {
            throw new IllegalStateException(
                    "There are no format attributes configured.  Writing all entity fields to the output.");
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
        componentStatistics.incrementOutboundMessages();
        outputMessage.getHeader().setSequenceNumber(componentStatistics.getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
    }

    private String processInputRow(EntityData inputRow) {
        StringBuilder stringBuilder = new StringBuilder();
        for (AttributeFormat attribute : attributesList) {
            Object value = inputRow.get(attribute.getAttributeId());
            stringBuilder.append(value != null ? value.toString() : "");
        }
        return stringBuilder.toString();
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
                    attributesMap.put(
                            attributeSetting.getAttributeId(),
                            new AttributeFormat(attributeSetting.getAttributeId(), Integer
                                    .parseInt(attributeSetting.getValue()), -1));
                }
            } else if (attributeSetting.getName().equalsIgnoreCase(
                    FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH)) {
                if (attributesMap.containsKey(attributeSetting.getAttributeId())) {
                    attributesMap.get(attributeSetting.getAttributeId()).setLength(
                            Integer.parseInt(attributeSetting.getValue()));
                } else {
                    attributesMap.put(
                            attributeSetting.getAttributeId(),
                            new AttributeFormat(attributeSetting.getAttributeId(), -1, Integer
                                    .parseInt(attributeSetting.getValue())));
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

        String attributeId;
        int ordinal;
        int length;

        public AttributeFormat(String attributeId, int ordinal, int length) {
            this.attributeId = attributeId;
            this.ordinal = ordinal;
            this.length = length;
        }

        public String getAttributeId() {
            return attributeId;
        }

        public void setAttributeId(String attributeId) {
            this.attributeId = attributeId;
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
