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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public abstract class AbstractRdbmsComponentTest extends AbstractComponentRuntimeTestSupport {

	List<String> sqls;
	String expectedFlowReplacementSql;
	Map<String, Object> expectedParamMap;
	long rowsPerMessage;
    boolean trimColumns;
    boolean matchOnColumnNameOnly;
    Message resultMessage;
    
	@Before
	public void reset() {
		super.reset();
		sqls = new ArrayList<String>();
		expectedFlowReplacementSql = "";
		expectedParamMap = new HashMap<String, Object>();
		rowsPerMessage = 0L;
	    trimColumns = false;
	    matchOnColumnNameOnly = false;
	    resultMessage = new Message("resultMessage");
	}
	
	abstract protected boolean sqlRequired();
	
	@Test
    public void testStartWhenSqlNotSet() {
        setupStart(new SettingsBuilder().build());
        if (sqlRequired()) {
            try {
                properties.setProperty(AbstractRdbmsComponentRuntime.SQL, "");
                spy.start(0, context);
                fail("Should have gotten a misconfigured exception");
            } catch (MisconfiguredException ex) {

            }
        }
    }
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
    public void setupHandle() {
		
		super.setupHandle();
		
		NamedParameterJdbcTemplate mJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		
		// Verify manipulatedFlow parameters as a string map were replaced
		if (flowParametersAsString != null && flowParametersAsString.size() > 0) {
			when(mJdbcTemplate.query(anyString(), anyMap(), Mockito.any(ResultSetExtractor.class))).thenAnswer(new Answer() {
				public Object answer(InvocationOnMock invocation) {
					Object[] args = invocation.getArguments();
					
					// Verify manipulatedFlow parameters were replaced
					assertEquals(expectedFlowReplacementSql, args[0]);
					
					return resultMessage;
				}
			});
		}
		// Verify manipulatedFlow parameters as a serialized map were set
		else if (flowParameters != null && flowParameters.size() > 0) {
			when(mJdbcTemplate.query(anyString(), anyMap(), Mockito.any(ResultSetExtractor.class))).thenAnswer(new Answer() {
				public Object answer(InvocationOnMock invocation) {
					Object[] args = invocation.getArguments();
					
					
					
					if (((Map) args[1]).size() > 0) {
						Map<String, Object> paramMap = (Map<String, Object>) args[1];
						for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
							Map<String, Object> expectedMap = expectedParamMap;
							assertTrue(expectedMap.containsKey(entry.getKey()));
							
							Object expectedValue = expectedMap.get(entry.getKey());
							Object actualValue = entry.getValue();
							
							assertEquals("Param map to be used with sql executed does not match for key : " + entry.getKey(), expectedValue, actualValue);
						}
					}
					return resultMessage;
				}
			});
		} else {
			if (messages != null && messages.size() > 0) {
				when(mJdbcTemplate.query(anyString(), anyMap(), Mockito.any(ResultSetExtractor.class))).thenReturn(messages.get(0).getInputMessage());
			}
		}
		doReturn(mJdbcTemplate).when((AbstractRdbmsComponentRuntime) spy).getJdbcTemplate();
		
	}
}
