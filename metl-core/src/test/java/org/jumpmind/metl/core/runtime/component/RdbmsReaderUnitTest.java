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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.ShutdownMessage;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.properties.TypedProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class RdbmsReaderUnitTest extends AbstractRdbmsComponentTest {

	
	@Test
	@Override
	public void testHandleStartupMessage() {
		inputMessage = new StartupMessage();
		
		runHandle();
		assertHandle(0, 1, 1, 0);
	}

	@Test
	@Override
	public void testHandleShutdownMessage() {
		inputMessage = new ShutdownMessage("test");
		
		runHandle();
		assertHandle(0, 1, 1, 0);
	}
	
	@Test
	public void testReceivesStartupWithResults() {
		inputMessage = new StartupMessage();
		
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from test");
		this.sqls = sqls;
		
		runHandle();
		assertHandle(1, 1, 0, 0);
	}
	
	@Test
	@Override
	public void testHandleEmptyPayload() {
		setupHandle();
		runHandle();
		assertHandle(0, 1, 0, 0);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkInputMessage() {
		setupHandle();
		
		inputMessage.setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkFlow() {
		setupHandle();
		
		inputMessage.setPayload(new ArrayList<EntityData>());
		unitOfWorkLastMessage = true;
		
		runHandle();
		assertHandle(1, 1, 1, 0, true);
	}

	@Test
	@Override
	public void testHandleNormal() {
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from $(UNIT_TEST)");
		
		this.sqls = sqls;
		
		runHandle();
		assertHandle(1, 1, 0, 0);
	}

	
	@Test
	public void testHandleWithFlowParameters() {
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from $(UNIT_TEST)");
		
		Map<String, String> flowParameters = new HashMap<String, String>();
		flowParameters.put("UNIT_TEST", "GOES_BOOM");
		
		this.sqls = sqls;
		this.flowParametersAsString = flowParameters;
		expectedFlowReplacementSql = "select * from GOES_BOOM";
		
		runHandle();
		assertHandle(1, 1, 0, 0);
	}
	
	@Test
	public void testHandleSettingParamsFromHeaderNoPayload() {
		Map<String, Serializable> flowParameters = new HashMap<String, Serializable>();
		flowParameters.put("key", "value");
		
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from test");
		
		Map<String, Object> expectedParams = new HashMap<String, Object>();
		expectedParams.putAll(flowParameters);
		
		this.sqls = sqls;
		this.flowParameters = flowParameters;
		expectedParamMap = expectedParams;
		
		runHandle();
		assertHandle(1, 1, 0, 0);
	}
	
	@Test
	public void testApplySettings() {
		// setup
		RdbmsReader reader = spy(new RdbmsReader());
		
		String eSql = "select * from test";
		long eRowsPerMessage = 5l;
		String eTrimColumns = "true";
		String eMatchOnColumnNameOnly = "true";
		
		TypedProperties eProperties = new TypedProperties();
		eProperties.setProperty(RdbmsReader.SQL, eSql);
		eProperties.setProperty(RdbmsReader.ROWS_PER_MESSAGE, eRowsPerMessage);
		eProperties.setProperty(RdbmsReader.TRIM_COLUMNS, eTrimColumns);
		eProperties.setProperty(RdbmsReader.MATCH_ON_COLUMN_NAME_ONLY, eMatchOnColumnNameOnly);
				
		doReturn(eProperties).when(reader).getTypedProperties();
		
		// actual
		reader.start();
		assertTrue(reader.isMatchOnColumnNameOnly());
		assertTrue(reader.isTrimColumns());
		assertEquals(eRowsPerMessage, reader.getRowsPerMessage());
		assertEquals(1, reader.getSqls().size());
		assertEquals(eSql, reader.getSqls().get(0));
	}

	@Override
	public IComponentRuntime getComponentSpy() {
		RdbmsReader reader = spy(new RdbmsReader());
		return reader;
		
	}

	@Override
	public void setupHandle() {
		super.setupHandle();
		
		doReturn(this.sqls).when((RdbmsReader) spy).getSqls();
		
	}
	
	
}
