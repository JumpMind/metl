package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;

public class FixedLengthParser extends AbstractComponentRuntime {

    public static final String TYPE = "Parse Fixed";

    public final static String SETTING_HEADER_LINES_TO_SKIP = "header.lines.to.skip";

    public final static String SETTING_FOOTER_LINES_TO_SKIP = "footer.lines.to.skip";

    int numberOfFooterLinesToSkip = 0;

    int numberOfHeaderLinesToSkip = 0;

    List<AttributeFormat> attributesList;

    @Override
    protected void start() {
        if (getComponent().getOutputModel() == null) {
            throw new IllegalStateException("This component requires an output model.  Please select one.");
        }
        numberOfFooterLinesToSkip = getComponent().getInt(SETTING_FOOTER_LINES_TO_SKIP, 0);
        numberOfHeaderLinesToSkip = getComponent().getInt(SETTING_HEADER_LINES_TO_SKIP, 0);
        convertAttributeSettingsToAttributeFormat();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget, boolean unitOfWorkLastMessage) {
        getComponentStatistics().incrementInboundMessages();

        ArrayList<String> inputRows = inputMessage.getPayload();

        ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();
        Message outputMessage = inputMessage.clone(getFlowStepId(), outputPayload);
        int headerRowsToSkip = inputMessage.getHeader().getSequenceNumber() == 0 ? numberOfHeaderLinesToSkip : 0;
        try {
            int rowCount = 0;
            for (String inputRow : inputRows) {
                if (headerRowsToSkip == 0) {
                    if (!inputMessage.getHeader().isUnitOfWorkLastMessage() || (rowCount + numberOfFooterLinesToSkip < inputRows.size())) {
                        EntityData data = processInputRow(inputRow);
                        if (data != null) {
                            getComponentStatistics().incrementNumberEntitiesProcessed();
                            outputPayload.add(data);
                        }
                    }
                } else {
                    headerRowsToSkip--;
                }
                rowCount++;
            }
        } catch (IOException e) {
            throw new IoException(e);
        }

        getComponentStatistics().incrementOutboundMessages();
        outputMessage.getHeader().setSequenceNumber(getComponentStatistics().getNumberOutboundMessages());
        outputMessage.getHeader().setUnitOfWorkLastMessage(inputMessage.getHeader().isUnitOfWorkLastMessage());
        messageTarget.put(outputMessage);
    }

    private EntityData processInputRow(String inputRow) throws IOException {
        if (attributesList.size() > 0) {
            EntityData data = new EntityData();
            for (AttributeFormat attribute : attributesList) {
                int length = attribute.getLength() > inputRow.length() ? inputRow.length() : attribute.getLength();
                Object value = inputRow.substring(0, length).trim();
                inputRow = inputRow.substring(length);
                if (isNotBlank(attribute.getFormatFunction())) {
                    value = ModelAttributeScriptHelper.eval(attribute.getAttribute(), value, attribute.getEntity(), data,
                            attribute.getFormatFunction());
                }

                data.put(attribute.getAttributeId(), value);
            }
            return data;
        } else {
            return null;
        }
    }

    private void convertAttributeSettingsToAttributeFormat() {

        Map<String, AttributeFormat> attributesMap = new HashMap<String, AttributeFormat>();

        List<ComponentAttributeSetting> attributeSettings = getComponent().getAttributeSettings();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            if (!attributesMap.containsKey(attributeSetting.getAttributeId())) {
                Model model = getComponent().getOutputModel();
                ModelAttribute attribute = model.getAttributeById(attributeSetting.getAttributeId());
                ModelEntity entity = model.getEntityById(attribute.getEntityId());
                attributesMap.put(attributeSetting.getAttributeId(), new AttributeFormat(attribute, entity));
            }

            if (attributeSetting.getName().equalsIgnoreCase(FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_ORDINAL)) {
                attributesMap.get(attributeSetting.getAttributeId()).setOrdinal(Integer.parseInt(attributeSetting.getValue()));
            } else if (attributeSetting.getName().equalsIgnoreCase(FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_LENGTH)) {
                attributesMap.get(attributeSetting.getAttributeId()).setLength(Integer.parseInt(attributeSetting.getValue()));
            } else if (attributeSetting.getName().equalsIgnoreCase(FixedLengthFormatter.FIXED_LENGTH_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION)) {
                attributesMap.get(attributeSetting.getAttributeId()).setFormatFunction(attributeSetting.getValue());
            }
        }

        attributesList = new ArrayList<AttributeFormat>(attributesMap.values());

        Collections.sort(attributesList, new Comparator<AttributeFormat>() {
            public int compare(AttributeFormat format1, AttributeFormat format2) {
                return format1.getOrdinal() - format2.getOrdinal();
            }
        });
    }

    private class AttributeFormat {

        ModelAttribute attribute;
        ModelEntity entity;
        int ordinal;
        int length;
        String formatFunction;

        public AttributeFormat(ModelAttribute attribute, ModelEntity entity) {
            this.attribute = attribute;
            this.entity = entity;
        }

        public String getAttributeId() {
            return attribute.getId();
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

        public void setFormatFunction(String formatFunction) {
            this.formatFunction = formatFunction;
        }

        public String getFormatFunction() {
            return formatFunction;
        }

        public ModelAttribute getAttribute() {
            return attribute;
        }

        public ModelEntity getEntity() {
            return entity;
        }
    }

}
