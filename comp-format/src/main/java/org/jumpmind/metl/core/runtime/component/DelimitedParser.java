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
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.symmetric.csv.CsvReader;

import groovy.json.StringEscapeUtils;

public class DelimitedParser extends AbstractComponentRuntime {

    public static final String TYPE = "Parse Delimited";

    public final static String SETTING_DELIMITER = "delimiter";

    public final static String SETTING_QUOTE_CHARACTER = "quote.character";

    public final static String SETTING_HEADER_LINES_TO_SKIP = "header.lines.to.skip";

    public final static String SETTING_FOOTER_LINES_TO_SKIP = "footer.lines.to.skip";

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
    public void start() {
        delimiter = StringEscapeUtils.unescapeJava(getComponent().get(SETTING_DELIMITER, delimiter));
        quoteCharacter = getComponent().get(SETTING_QUOTE_CHARACTER, quoteCharacter);
        encoding = getComponent().get(SETTING_ENCODING, encoding);
        numberOfFooterLinesToSkip = getComponent().getInt(SETTING_FOOTER_LINES_TO_SKIP, 0);
        numberOfHeaderLinesToSkip = getComponent().getInt(SETTING_HEADER_LINES_TO_SKIP, 0);
        convertAttributeSettingsToAttributeFormat();
        if (getComponent().getOutputModel() == null) {
            throw new IllegalStateException("This component requires an output model.  Please select one.");
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof TextMessage) {
            ArrayList<String> inputRows = ((TextMessage)inputMessage).getPayload();

            ArrayList<EntityData> outputPayload = new ArrayList<EntityData>();
            int headerRowsToSkip = inputMessage.getHeader().getSequenceNumber() == 0 ? numberOfHeaderLinesToSkip : 0;
            try {
                int rowCount = 0;
                if (inputRows != null) {
                    StringBuilder combined = new StringBuilder();
                    for (String inputRow : inputRows) {
                        if (headerRowsToSkip == 0) {                            
                            if (rowCount + numberOfFooterLinesToSkip < inputRows.size()) {
                                combined.append(inputRow).append("\n");
                            }
                        } else {
                            headerRowsToSkip--;
                        }
                        rowCount++;
                    }
                    
                    processInputRows(inputMessage, combined, outputPayload);

                }
            } catch (IOException e) {
                throw new IoException(e);
            }

            callback.sendEntityDataMessage(null, outputPayload);
        }
    }

    private void processInputRows(Message inputMessage, StringBuilder inputRow, List<EntityData> payload) throws IOException {

        CsvReader csvReader = new CsvReader(new ByteArrayInputStream(inputRow.toString().getBytes(Charset.forName(encoding))), Charset.forName(encoding));
        csvReader.setDelimiter(delimiter.charAt(0));
        if (isNotBlank(quoteCharacter)) {
            csvReader.setTextQualifier(quoteCharacter.charAt(0));
            csvReader.setUseTextQualifier(true);
        } else {
            csvReader.setUseTextQualifier(false);
        }
        while (csvReader.readRecord()) {
            EntityData data = new EntityData();
            if (attributes.size() > 0) {
                for (AttributeFormat attribute : attributes) {
                    Object value = csvReader.get(attribute.getOrdinal() - 1);
                    if (isNotBlank(attribute.getFormatFunction())) {
                        value = ModelAttributeScriptHelper.eval(inputMessage, context, attribute.getAttribute(), value, getOutputModel(), attribute.getEntity(),
                                data, attribute.getFormatFunction());
                    }

                    data.put(attribute.getAttributeId(), value);
                }
            } else {
                Model model = getComponent().getOutputModel();
                List<ModelEntity> entities = model.getModelEntities();
                int index = 0;
                for (ModelEntity modelEntity : entities) {
                    List<ModelAttrib> attributes = modelEntity.getModelAttributes();
                    for (ModelAttrib modelAttribute : attributes) {
                        data.put(modelAttribute.getId(), csvReader.get(index));
                        index++;
                    }
                }
            }

            context.getComponentStatistics().incrementNumberEntitiesProcessed(getThreadNumber());
            payload.add(data);
        }

    }

    private void convertAttributeSettingsToAttributeFormat() {
        List<ComponentAttribSetting> attributeSettings = getComponent().getAttributeSettings();
        Map<String, AttributeFormat> formats = new HashMap<String, DelimitedParser.AttributeFormat>();
        for (ComponentAttribSetting attributeSetting : attributeSettings) {
            AttributeFormat format = formats.get(attributeSetting.getAttributeId());
            if (format == null) {
                Model inputModel = getComponent().getOutputModel();
                ModelAttrib attribute = inputModel.getAttributeById(attributeSetting.getAttributeId());
                if (attribute != null) {
                    ModelEntity entity = inputModel.getEntityById(attribute.getEntityId());
                    format = new AttributeFormat(attributeSetting.getAttributeId(), entity, attribute);
                    formats.put(attributeSetting.getAttributeId(), format);
                }
            }

            if (format != null) {
                if (attributeSetting.getName().equalsIgnoreCase(DELIMITED_FORMATTER_ATTRIBUTE_ORDINAL)) {
                    format.setOrdinal(Integer.parseInt(attributeSetting.getValue()));
                } else if (attributeSetting.getName().equalsIgnoreCase(DELIMITED_FORMATTER_ATTRIBUTE_FORMAT_FUNCTION)) {
                    format.setFormatFunction(attributeSetting.getValue());
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

    protected class AttributeFormat {

        public AttributeFormat(String attributeId, ModelEntity entity, ModelAttrib attribute) {
            this.attributeId = attributeId;
            this.entity = entity;
            this.attribute = attribute;
        }

        ModelEntity entity;

        ModelAttrib attribute;

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

        public ModelAttrib getAttribute() {
            return attribute;
        }

        public ModelEntity getEntity() {
            return entity;
        }
    }

}
