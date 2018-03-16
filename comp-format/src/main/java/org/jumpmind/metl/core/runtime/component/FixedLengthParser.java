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

import java.io.IOException;
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

public class FixedLengthParser extends AbstractComponentRuntime {

    public static final String TYPE = "Parse Fixed";

    public final static String SETTING_HEADER_LINES_TO_SKIP = "header.lines.to.skip";

    public final static String SETTING_FOOTER_LINES_TO_SKIP = "footer.lines.to.skip";

    public final static String SETTING_TRIM_PARSED_COLUMN = "trim.parsed.column";

    int numberOfFooterLinesToSkip = 0;

    int numberOfHeaderLinesToSkip = 0;

    List<AttributeFormat> attributesList;

    @Override
    public void start() {
        if (getComponent().getOutputModel() == null) {
            throw new IllegalStateException("This component requires an output model.  Please select one.");
        }
        numberOfFooterLinesToSkip = getComponent().getInt(SETTING_FOOTER_LINES_TO_SKIP, 0);
        numberOfHeaderLinesToSkip = getComponent().getInt(SETTING_HEADER_LINES_TO_SKIP, 0);
        convertAttributeSettingsToAttributeFormat();
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
                for (String inputRow : inputRows) {
                    if (headerRowsToSkip == 0) {
                        // TODO what if the file is split across messages? this
                        // logic would not work
                        if (rowCount + numberOfFooterLinesToSkip < inputRows.size()) {
                            EntityData data = processInputRow(inputMessage, inputRow);
                            if (data != null) {
                                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);
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

            callback.sendEntityDataMessage(null, outputPayload);
        }
    }

    private EntityData processInputRow(Message inputMessage, String inputRow) throws IOException {
        if (attributesList.size() > 0) {
            EntityData data = new EntityData();
            for (AttributeFormat attribute : attributesList) {
                int length = attribute.getLength() > inputRow.length() ? inputRow.length() : attribute.getLength();
                Object value = null;
                if (properties.is(SETTING_TRIM_PARSED_COLUMN, true)) {
                	value = inputRow.substring(0, length).trim();
                } else {
                	value = inputRow.substring(0, length);
                }

                inputRow = inputRow.substring(length);
                if (isNotBlank(attribute.getFormatFunction())) {
                    value = ModelAttributeScriptHelper.eval(inputMessage, context, attribute.getAttribute(), value, getOutputModel(), attribute.getEntity(), data,
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

        List<ComponentAttribSetting> attributeSettings = getComponent().getAttributeSettings();
        for (ComponentAttribSetting attributeSetting : attributeSettings) {
            if (!attributesMap.containsKey(attributeSetting.getAttributeId())) {
                Model model = getComponent().getOutputModel();
                ModelAttrib attribute = model.getAttributeById(attributeSetting.getAttributeId());
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

        ModelAttrib attribute;
        ModelEntity entity;
        int ordinal;
        int length;
        String formatFunction;

        public AttributeFormat(ModelAttrib attribute, ModelEntity entity) {
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

        public ModelAttrib getAttribute() {
            return attribute;
        }

        public ModelEntity getEntity() {
            return entity;
        }
    }

}
