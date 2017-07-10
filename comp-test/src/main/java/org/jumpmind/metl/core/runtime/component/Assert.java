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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.sql.DataSource;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.BinaryMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.properties.TypedProperties;
import org.springframework.jdbc.core.JdbcTemplate;

public class Assert extends AbstractComponentRuntime {

    public static final String EXPECTED_ENTITY_MESSAGE_COUNT = "expected.entity.messages.count";
    public static final String EXPECTED_ENTITY_COUNT_PER_MESSAGE = "expected.entity.couunt.per.message";
    public static final String EXPECTED_TEXT_MESSAGE_COUNT = "expected.text.messages.count";
    public static final String EXPECTED_BINARY_MESSAGE_COUNT = "expected.binary.messages.count";
    public static final String EXPECTED_CONTROL_MESSAGE_COUNT = "expected.control.messages.count";
    public static final String EXPECTED_DISTINCT_ENTITY_TYPE_COUNT = "expected.distinct.entities.count";
    public static final String EXPECTED_DISTINCT_ATTRIBUTE_TYPE_COUNT = "expected.distinct.attributes.count";
    public static final String EXPECTED_EMPTY_PAYLOAD_MESSAGE_COUNT = "expected.empty.payload.messages.count";
    public static final String EXPECTED_CUSTOM_HEADER_PAIRS = "expected.custom.header.pairs";
    public static final String EXPECTED_CUSTOM_CONTROL_HEADER_PAIRS = "expected.custom.control.header.pairs";
    public static final String EXPECTED_TEXT_PAYLOAD = "expected.text.payload";
    public static final String EXPECTED_SQL_COUNT = "expected.sql.count";
    public static final String ASSERT_SQL = "sql";
    public static final String ASSERT_SQL_DATASOURCE = "sql.datasource";

    int expectedEntityMessageCount = 0;
    int expectedDistinctEntityTypeCount = 0;
    int expectedDistinctAttributeTypeCount = 0;
    int expectedTextMessageCount = 0;
    int expectedBinaryMessageCount = 0;
    int expectedControlMessageCount = 0;
    int expectedEmptyPayloadMessageCount = 0;
    int expectedSqlCount = 0;
    String expectedCustomHeaderPairs;
    String expectedCustomControlMsgHeaderPairs;
    Long expectedEntityCountPerMessage;
    String expectedTextPayload;

    String sql;
    String dataSourceId;

    int entityMessageCount = 0;
    int textMessageCount = 0;
    int binaryMessageCount = 0;
    int controlMessageCount = 0;
    int emptyPayloadMessageCount = 0;
    int entityCountPerMessage = 0;
    StringBuilder textPayload = new StringBuilder();
    StringBuilder messageHeaders = new StringBuilder();
    StringBuilder controlMessageHeaders = new StringBuilder();
    
    Set<String> distinctEntityIds = new HashSet<>();
    Set<String> distinctAttributeIds = new HashSet<>();

    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        expectedCustomControlMsgHeaderPairs = properties.get(EXPECTED_CUSTOM_CONTROL_HEADER_PAIRS, null);
        expectedCustomHeaderPairs = properties.get(EXPECTED_CUSTOM_HEADER_PAIRS, null);
        expectedDistinctAttributeTypeCount = properties.getInt(EXPECTED_DISTINCT_ATTRIBUTE_TYPE_COUNT, -1);
        expectedDistinctEntityTypeCount = properties.getInt(EXPECTED_DISTINCT_ENTITY_TYPE_COUNT, -1);
        expectedControlMessageCount = properties.getInt(EXPECTED_CONTROL_MESSAGE_COUNT,
                expectedEntityMessageCount);
        expectedTextMessageCount = properties.getInt(EXPECTED_TEXT_MESSAGE_COUNT,
                expectedTextMessageCount);
        expectedEntityMessageCount = properties.getInt(EXPECTED_ENTITY_MESSAGE_COUNT,
                expectedControlMessageCount);
        expectedEmptyPayloadMessageCount = properties.getInt(EXPECTED_EMPTY_PAYLOAD_MESSAGE_COUNT,
                expectedEmptyPayloadMessageCount);
        expectedBinaryMessageCount = properties.getInt(EXPECTED_BINARY_MESSAGE_COUNT,
                expectedBinaryMessageCount);
        expectedSqlCount = properties.getInt(EXPECTED_SQL_COUNT, expectedSqlCount);
        expectedEntityCountPerMessage = properties.getLong(EXPECTED_ENTITY_COUNT_PER_MESSAGE);
        expectedTextPayload = properties.get(EXPECTED_TEXT_PAYLOAD, null);
        sql = properties.get(ASSERT_SQL);
        dataSourceId = properties.get(ASSERT_SQL_DATASOURCE);
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof ControlMessage) {
            controlMessageCount++;
            if (controlMessageCount > expectedControlMessageCount) {
                throw new AssertException("\nFlow Step: '" + this.context.getFlowStep().getName() + 
                        String.format("'\nThe expected %d control message count has been exceeded.",
                                expectedControlMessageCount));
            }
        } else if (inputMessage instanceof EntityDataMessage) {
            ArrayList<EntityData> payload = ((EntityDataMessage) inputMessage).getPayload();
            entityCountPerMessage = payload.size();
            entityMessageCount++;
            if (entityMessageCount > expectedEntityMessageCount) {
                throw new AssertException("\nFlow Step: '" + this.context.getFlowStep().getName() + 
                        String.format("'\nThe expected %d entity message count has been exceeded.",
                                expectedEntityMessageCount));
            }
            Model inputModel = getInputModel();
            if (inputModel != null) {
                for (EntityData entityData : payload) {
                    Set<String> attributeIds = entityData.keySet();
                    for (String attributeId : attributeIds) {
                        distinctEntityIds.add(inputModel.getAttributeById(attributeId).getEntityId());                        
                    }
                    distinctAttributeIds.addAll(attributeIds);
                }
            }
        } else if (inputMessage instanceof TextMessage) {
            TextMessage textMessage = (TextMessage)inputMessage;
            List<String> payload = textMessage.getPayload();
            for (String string : payload) {
                textPayload.append(string).append("\n");
            }
            textMessageCount++;   
            if (textMessageCount > expectedTextMessageCount) {
                throw new AssertException("\nFlow Step: '" + this.context.getFlowStep().getName() + 
                        String.format("'\nThe expected %d text message count has been exceeded.",
                                expectedTextMessageCount));
            }      
        } else if (inputMessage instanceof BinaryMessage) {
            binaryMessageCount++;
            if (binaryMessageCount > expectedBinaryMessageCount) {
                throw new AssertException("\nFlow Step: '" + this.context.getFlowStep().getName() + 
                        String.format("'\nThe expected %d binary message count has been exceeded.",
                                expectedBinaryMessageCount));
            }  
        } else {
            emptyPayloadMessageCount++;
            if (emptyPayloadMessageCount > expectedEmptyPayloadMessageCount) {
                throw new AssertException("\nFlow Step: '" + this.context.getFlowStep().getName() + 
                        String.format("'\nThe expected %d empty payload message count has been exceeded.",
                                expectedEmptyPayloadMessageCount));
            }  
        }
        
        if (!(inputMessage instanceof ControlMessage)) {
            Map<String,String> headerValues = inputMessage.getHeader().getAsStrings();
            boolean first = true;
            for (String key : headerValues.keySet()) {
                if (!key.startsWith("_")) {
                    if (!first) {
                        messageHeaders.append(",");
                    }
                    messageHeaders.append(key).append("=").append(headerValues.get(key));
                    first = false;
                }
            }
            if (!first) {
                messageHeaders.append("\n");
            }
        } else {
            Map<String,String> headerValues = inputMessage.getHeader().getAsStrings();
            boolean first = true;
            for (String key : headerValues.keySet()) {
                if (!key.startsWith("_")) {
                    if (!first) {
                        controlMessageHeaders.append(",");
                    }
                    controlMessageHeaders.append(key).append("=").append(headerValues.get(key));
                    first = false;
                }
            }
            if (!first) {
                controlMessageHeaders.append("\n");
            }
        }

        callback.forward(inputMessage);
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void flowCompleted(boolean cancelled) {
        StringBuilder assertFailed = new StringBuilder();
        if (expectedControlMessageCount != controlMessageCount) {
            assertFailed.append(String.format("\nExpected %d control messages but received %s.",
                    expectedControlMessageCount, controlMessageCount));
        }

        if (expectedEmptyPayloadMessageCount != emptyPayloadMessageCount) {
            assertFailed.append(String.format("\nExpected %d empty messages but received %s.",
                    expectedEmptyPayloadMessageCount, emptyPayloadMessageCount));
        }

        if (expectedEntityMessageCount != entityMessageCount) {
            assertFailed.append(String.format("\nExpected %d entity messages but received %s.",
                    expectedEntityMessageCount, entityMessageCount));
        }

        if (expectedTextMessageCount != textMessageCount) {
            assertFailed.append(String.format("\nExpected %d text messages but received %s.",
                    expectedTextMessageCount, textMessageCount));
        }

        if (expectedBinaryMessageCount != binaryMessageCount) {
            assertFailed.append(String.format("\nExpected %d binary messages but received %s.",
                    expectedBinaryMessageCount, binaryMessageCount));
        }
        
        if (expectedDistinctAttributeTypeCount != -1 && expectedDistinctAttributeTypeCount != distinctAttributeIds.size()) {
            assertFailed.append(String.format("\nExpected %d distinct attribute types but received %s.",
                   expectedDistinctAttributeTypeCount, distinctAttributeIds.size()));
        }
        
        if (expectedDistinctEntityTypeCount != -1 && expectedDistinctEntityTypeCount != distinctEntityIds.size()) {
            assertFailed.append(String.format("\nExpected %d distinct entity types but received %s.",
                    expectedDistinctEntityTypeCount, distinctEntityIds.size()));
        }

        if (expectedEntityCountPerMessage.intValue() != -1
                && expectedEntityCountPerMessage.intValue() != entityCountPerMessage) {
            assertFailed.append(String.format("\nExpected %d entities per message but received %s.",
                    expectedEntityCountPerMessage.intValue(), entityCountPerMessage));
        }
        
        if (isNotBlank(expectedTextPayload) && !expectedTextPayload.trim().equals(textPayload.toString().trim())) {
            assertFailed.append(String.format("\nExpected text payload of:\n%s \nReceived:\n%s",
                    expectedTextPayload, textPayload.toString().trim()));       
        }
        
        if (isNotBlank(expectedCustomHeaderPairs) && !expectedCustomHeaderPairs.trim().equals(messageHeaders.toString().trim())) {
            assertFailed.append(String.format("\nExpected the following headers of:\n%s \nReceived:\n%s",
                    expectedCustomHeaderPairs.trim(), messageHeaders.toString().trim()));                   
        }
        
        if (isNotBlank(expectedCustomControlMsgHeaderPairs) && !expectedCustomControlMsgHeaderPairs.trim().equals(controlMessageHeaders.toString().trim())) {
            assertFailed.append(String.format("\nExpected the following control headers of:\n%s \nReceived:\n%s",
                    expectedCustomControlMsgHeaderPairs.trim(), controlMessageHeaders.toString().trim()));                   
        }

        if (isNotBlank(sql)) {
            IResourceRuntime targetResource = context.getDeployedResources().get(dataSourceId);
            DataSource ds = targetResource.reference();
            JdbcTemplate template = new JdbcTemplate(ds);
            int sqlCount = template.queryForObject(sql, Integer.class);
            if (expectedSqlCount != sqlCount) {
                assertFailed.append(String.format("\nExpected %d sql count but received %s.",
                        expectedSqlCount, sqlCount));
            }
        }

        if (assertFailed.length() > 0) {
            throw new AssertException("\nFlow Step: " + this.context.getFlowStep().getName() + assertFailed.toString());
        }
    }
}
