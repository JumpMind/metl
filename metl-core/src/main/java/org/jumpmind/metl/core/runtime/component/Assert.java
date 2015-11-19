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

import javax.sql.DataSource;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.util.ComponentUtils;
import org.jumpmind.properties.TypedProperties;
import org.springframework.jdbc.core.JdbcTemplate;

public class Assert extends AbstractComponentRuntime {
    
    public static final String  EXPECTED_ENTITY_MESSAGE_COUNT = "expected.entity.messages.count";
    public static final String  EXPECTED_TEXT_MESSAGE_COUNT = "expected.text.messages.count";
    public static final String  EXPECTED_CONTROL_MESSAGE_COUNT = "expected.control.messages.count";
    public static final String  EXPECTED_EMPTY_PAYLOAD_MESSAGE_COUNT = "expected.empty.payload.messages.count";
    public static final String  EXPECTED_SQL_COUNT = "expected.sql.count";
    public static final String  ASSERT_SQL = "sql";
    public static final String  ASSERT_SQL_DATASOURCE = "sql.datasource";
    
    int expectedEntityMessageCount = 0;
    int expectedTextMessageCount = 0;
    int expectedControlMessageCount = 0;
    int expectedEmptyPayloadMessageCount = 0;
    int expectedSqlCount = 0;
    
    String sql;
    String dataSourceId;
    
    int entityMessageCount = 0;
    int textMessageCount = 0;
    int controlMessageCount = 0;
    int emptyPayloadMessageCount = 0;

    
    @Override
    protected void start() {
        TypedProperties properties = getTypedProperties();
        expectedControlMessageCount = properties.getInt(EXPECTED_CONTROL_MESSAGE_COUNT, expectedEntityMessageCount);
        expectedTextMessageCount = properties.getInt(EXPECTED_TEXT_MESSAGE_COUNT, expectedTextMessageCount);
        expectedEntityMessageCount = properties.getInt(EXPECTED_ENTITY_MESSAGE_COUNT, expectedControlMessageCount);
        expectedEmptyPayloadMessageCount = properties.getInt(EXPECTED_EMPTY_PAYLOAD_MESSAGE_COUNT, expectedEmptyPayloadMessageCount);
        expectedSqlCount = properties.getInt(EXPECTED_SQL_COUNT, expectedSqlCount);
        sql = properties.get(ASSERT_SQL);
        dataSourceId = properties.get(ASSERT_SQL_DATASOURCE);
    }

    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof ControlMessage) {
            controlMessageCount++;
        } else {
            if (ComponentUtils.getPayloadType(inputMessage.getPayload()) == ComponentUtils.PAYLOAD_TYPE_LIST_ENTITY) {
                entityMessageCount++;
            } else if(ComponentUtils.getPayloadType(inputMessage.getPayload()) == ComponentUtils.PAYLOAD_TYPE_LIST_STRING) {
                textMessageCount++;
            } else {
                emptyPayloadMessageCount++;
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
            assertFailed.append(String.format("\nExpected %d control messages but received %s.", expectedControlMessageCount, controlMessageCount));
        } 
        
        if (expectedEmptyPayloadMessageCount != emptyPayloadMessageCount) {
            assertFailed.append(String.format("\nExpected %d empty messages but received %s.", expectedEmptyPayloadMessageCount, emptyPayloadMessageCount));
        }
        
        if (expectedEntityMessageCount != entityMessageCount) {
            assertFailed.append(String.format("\nExpected %d entity messages but received %s.", expectedEntityMessageCount, entityMessageCount));
        }
        
        if (expectedTextMessageCount != textMessageCount) {
            assertFailed.append(String.format("\nExpected %d text messages but received %s.", expectedTextMessageCount, textMessageCount));
        }
        
        if (isNotBlank(sql)) {
            IResourceRuntime targetResource = context.getDeployedResources().get(dataSourceId);
            DataSource ds = targetResource.reference();
            JdbcTemplate template = new JdbcTemplate(ds);
            int sqlCount = template.queryForObject(sql, Integer.class);
            if (expectedSqlCount != sqlCount) {
                assertFailed.append(String.format("\nExpected %d sql count but received %s.", expectedSqlCount, sqlCount));
            }
        }
        
        if (assertFailed.length() > 0) {
            throw new AssertException(assertFailed.toString());
        }
    }


}
