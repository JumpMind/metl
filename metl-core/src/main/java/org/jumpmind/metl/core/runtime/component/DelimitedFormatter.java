package org.jumpmind.metl.core.runtime.component;

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
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.csv.CsvWriter;

public class DelimitedFormatter extends AbstractComponentRuntime {

    public static final String TYPE = "Format Delimited";

    public final static String DELIMITED_FORMATTER_DELIMITER = "delimited.formatter.delimiter";

    public final static String DELIMITED_FORMATTER_QUOTE_CHARACTER = "delimited.formatter.quote.character";

    public final static String DELIMITED_FORMATTER_WRITE_HEADER = "delimited.formatter.header";

    public final static String DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION = "delimited.formatter.attribute.format.function";

    public final static String DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL = "delimited.formatter.attribute.ordinal";

    String delimiter;

    String quoteCharacter;

    boolean useHeader;

    TypedProperties properties;

    List<AttributeFormat> attributes = new ArrayList<AttributeFormat>();

    @Override
    protected void start() {
        
        applySettings();
    }

    @Override
    public void handle( Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {

        if (attributes.size() == 0) {
            log(LogLevel.INFO, "There are no format attributes configured.  Writing all entity fields to the output");
        }

        ArrayList<EntityData> inputRows = inputMessage.getPayload();

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
        
        callback.sendMessage(outputPayload, unitOfWorkBoundaryReached);
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
        properties = getTypedProperties();
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
