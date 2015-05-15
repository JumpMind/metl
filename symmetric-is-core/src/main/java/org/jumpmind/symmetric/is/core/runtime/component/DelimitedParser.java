package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.csv.CsvReader;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(
        typeName = DelimitedParser.TYPE,
        category = ComponentCategory.PROCESSOR,
        iconImage = "delimitedformatter.png",
        inputMessage = MessageType.TEXT,
        outgoingMessage = MessageType.ENTITY)
public class DelimitedParser extends AbstractComponentRuntime {

    public static final String TYPE = "Parse Delimited";

    @SettingDefinition(
            order = 10,
            required = true,
            type = Type.TEXT,
            label = "Delimiter",
            defaultValue = ",")
    public final static String SETTING_DELIMITER = "delimiter";

    @SettingDefinition(
            order = 20,
            type = Type.TEXT,
            label = "Quote Character",
            defaultValue = "\"")
    public final static String SETTING_QUOTE_CHARACTER = "quote.character";
    
    @SettingDefinition(
            order = 30,
            type = Type.INTEGER,
            label = "Number of Header Lines to Skip",
            defaultValue = "0")
    public final static String SETTING_HEADER_LINES_TO_SKIP = "header.lines.to.skip";
    
    @SettingDefinition(
            order = 40,
            type = Type.INTEGER,
            label = "Number of Footer Lines to Skip",
            defaultValue = "0")
    public final static String SETTING_FOOTER_LINES_TO_SKIP = "footer.lines.to.skip";

    @SettingDefinition(order = 50, type = Type.TEXT, label = "Encoding", defaultValue = "UTF-8")
    public final static String SETTING_ENCODING = "encoding";

    public final static String DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION = DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION;

    public final static String DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL = DelimitedFormatter.DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL;

    String delimiter = ",";

    String quoteCharacter = "\"";

    String encoding = "UTF-8";
    
    int numberOfFooterLinesToSkip = 0;
    
    int numberOfHeaderLinesToSkip = 0;

    List<AttributeFormat> attributes = new ArrayList<AttributeFormat>();

    @Override
    protected void start() {
        delimiter = getComponent().get(SETTING_DELIMITER, delimiter);
        quoteCharacter = getComponent().get(SETTING_QUOTE_CHARACTER, quoteCharacter);
        encoding = getComponent().get(SETTING_ENCODING, encoding);
        numberOfFooterLinesToSkip = getComponent().getInt(SETTING_FOOTER_LINES_TO_SKIP, 0);
        numberOfHeaderLinesToSkip = getComponent().getInt(SETTING_HEADER_LINES_TO_SKIP, 0);
        convertAttributeSettingsToAttributeFormat();
        if (getComponent().getOutputModel() == null) {
            throw new IllegalStateException(
                    "This component requires an output model.  Please select one.");
        }
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        getComponentStatistics().incrementInboundMessages();

        ArrayList<String> inputRows = inputMessage.getPayload();

        ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();
        Message outputMessage = inputMessage.copy(getFlowStepId(), outputPayload);
        int headerRowsToSkip = numberOfHeaderLinesToSkip;
        try {
            int rowCount = 0;
            for (String inputRow : inputRows) {
                if (headerRowsToSkip == 0) {
                    if (rowCount + numberOfFooterLinesToSkip < inputRows.size()) {
                        EntityData data = processInputRow(inputRow);
                        if (data != null) {
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
        outputMessage.getHeader()
                .setSequenceNumber(getComponentStatistics().getNumberOutboundMessages());
        outputMessage.getHeader().setLastMessage(inputMessage.getHeader().isLastMessage());
        messageTarget.put(outputMessage);
    }

    private EntityData processInputRow(String inputRow) throws IOException {

        CsvReader csvReader = new CsvReader(new ByteArrayInputStream(inputRow.getBytes()),
                Charset.forName(encoding));
        csvReader.setDelimiter(delimiter.charAt(0));
        if (isNotBlank(quoteCharacter)) {
            csvReader.setTextQualifier(quoteCharacter.charAt(0));
            csvReader.setUseTextQualifier(true);
        }
        if (csvReader.readRecord()) {
            EntityData data = new EntityData();
            if (attributes.size() > 0) {
                for (AttributeFormat attribute : attributes) {
                    Object value = csvReader.get(attribute.getOrdinal() - 1);
                    if (isNotBlank(attribute.getFormatFunction())) {
                        value = ModelAttributeScriptHelper.eval(attribute.getAttribute(), value,
                                attribute.getEntity(), data, attribute.getFormatFunction());
                    }

                    data.put(attribute.getAttributeId(), value);
                }
            } else {
                Model model = getComponent().getOutputModel();
                List<ModelEntity> entities = model.getModelEntities();
                int index = 0;
                for (ModelEntity modelEntity : entities) {
                    List<ModelAttribute> attributes = modelEntity.getModelAttributes();
                    for (ModelAttribute modelAttribute : attributes) {
                        data.put(modelAttribute.getId(), csvReader.get(index));
                    }
                }
            }

            return data;
        }
        return null;

    }

    private void convertAttributeSettingsToAttributeFormat() {
        List<ComponentAttributeSetting> attributeSettings = getComponent().getAttributeSettings();
        Map<String, AttributeFormat> formats = new HashMap<String, DelimitedParser.AttributeFormat>();
        for (ComponentAttributeSetting attributeSetting : attributeSettings) {
            AttributeFormat format = formats.get(attributeSetting.getAttributeId());
            if (format == null) {
                Model inputModel = getComponent().getOutputModel();
                ModelAttribute attribute = inputModel.getAttributeById(attributeSetting
                        .getAttributeId());
                if (attribute != null) {
                    ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                    format = new AttributeFormat(attributeSetting.getAttributeId(), entity,
                            attribute);
                    formats.put(attributeSetting.getAttributeId(), format);                    
                    if (attributeSetting.getName().equalsIgnoreCase(DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL)) {
                        format.setOrdinal(Integer.parseInt(attributeSetting.getValue()));
                    } else if (attributeSetting.getName().equalsIgnoreCase(
                            DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION)) {
                        format.setFormatFunction(attributeSetting.getValue());
                    }
                }
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
