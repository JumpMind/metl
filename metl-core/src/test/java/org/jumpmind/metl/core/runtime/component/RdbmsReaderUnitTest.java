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
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class RdbmsReaderUnitTest extends AbstractRdbmsComponentTest {

    @Test
    public void testFindWhereInParameters() {
        RdbmsReader reader = new RdbmsReader();
        Set<String> attributeNames = reader.findWhereInParameters("select * from test where id in (:ENTITY.ATTRIBUTE1) and id is not null");
        assertEquals(1, attributeNames.size());
        assertEquals("ENTITY.ATTRIBUTE1", attributeNames.iterator().next());
        
        attributeNames = reader.findWhereInParameters("select * from test where id IN (:ENTITY.ATTRIBUTE1) and id is not null");
        assertEquals(1, attributeNames.size());
        assertEquals("ENTITY.ATTRIBUTE1", attributeNames.iterator().next());
        
        attributeNames = reader.findWhereInParameters("select * from test where id in  (:ENTITY.ATTRIBUTE1) and id is not null");
        assertEquals(1, attributeNames.size());
        assertEquals("ENTITY.ATTRIBUTE1", attributeNames.iterator().next());
        
        attributeNames = reader.findWhereInParameters("select * from test where id in  \n(:ENTITY.ATTRIBUTE1) and id is not null");
        assertEquals(1, attributeNames.size());
        assertEquals("ENTITY.ATTRIBUTE1", attributeNames.iterator().next());

        attributeNames = reader.findWhereInParameters("select * from test where id in  \n( :ENTITY.ATTRIBUTE1 ) and id is not null");
        assertEquals(1, attributeNames.size());
        assertEquals("ENTITY.ATTRIBUTE1", attributeNames.iterator().next());
        
        attributeNames = reader.findWhereInParameters("select * from test where id is not null");
        assertEquals(0, attributeNames.size());
        
        attributeNames = reader.findWhereInParameters("select * from test where id in  \n( :ENTITY.ATTRIBUTE1 ) and id2 not in (:ENTITY.ATTRIBUTE2)");
        assertEquals(2, attributeNames.size());
        Iterator<?> i = attributeNames.iterator();
        assertEquals("ENTITY.ATTRIBUTE1", i.next());
        assertEquals("ENTITY.ATTRIBUTE2", i.next());


    }
    
	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		try {
			((RdbmsReader) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof MisconfiguredException);
		}
	}

	@Test
	public void testStartDefaultsNoException() {
		setupStart(new SettingsBuilder().build());
		properties.setProperty(RdbmsReader.SQL, "select * from dual");
		
		((RdbmsReader) spy).start();
		
		List<String> expected = new ArrayList<String>();
		expected.add("select * from dual");
		
		TestUtils.assertList(expected, ((RdbmsReader) spy).sqls, false);
		Assert.assertEquals(-1, ((RdbmsReader) spy).rowsPerMessage);
		Assert.assertEquals(false, ((RdbmsReader) spy).trimColumns);
		Assert.assertEquals(false, ((RdbmsReader) spy).matchOnColumnNameOnly);
	}

	
	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder().build());
		
		properties.setProperty(RdbmsReader.SQL, "select * from dual");
		properties.setProperty(RdbmsReader.ROWS_PER_MESSAGE, "5");
		properties.setProperty(RdbmsReader.TRIM_COLUMNS, "true");
		properties.setProperty(RdbmsReader.MATCH_ON_COLUMN_NAME_ONLY, "true");
		
		((RdbmsReader) spy).start();
		
		Assert.assertEquals(5, ((RdbmsReader) spy).rowsPerMessage);
		Assert.assertEquals(true, ((RdbmsReader) spy).trimColumns);
		Assert.assertEquals(true, ((RdbmsReader) spy).matchOnColumnNameOnly);
	}
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new ControlMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}
	
	@Override
	protected boolean sqlRequired() {
	    return true;
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);		
		getInputMessage().setPayload(new ArrayList<EntityData>());		
		runHandle();
		/* 
		 * Expect one empty message when no data is selected
		 */
		assertHandle(0, getExpectedMessageMonitor(new MessageBuilder().withPayload(new PayloadBuilder().buildED()).build()));
	}
	
	@Test
	@Override
	public void testHandleNormal() {
		
	}
	/*
	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from $(UNIT_TEST)");
		this.sqls = sqls;
		
		// Messages
		Message message1 = new MessageBuilder("step1")
				.setPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.addKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
				.build()).buildED()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		// Expected
		ArrayList<EntityData> expectedPayload = new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.addKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
						.build()).buildED();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(1, 0, 0, 1, expectedPayload));
		
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}
	
	@Test
	public void testHandleWithFlowParameters() {
		// Setup
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from $(UNIT_TEST)");
		
		Map<String, String> flowParameters = new HashMap<String, String>();
		flowParameters.put("UNIT_TEST", "GOES_BOOM");
		
		this.sqls = sqls;
		this.flowParametersAsString = flowParameters;
		expectedFlowReplacementSql = "select * from GOES_BOOM";
		
		// Messages
		Message message1 = new MessageBuilder("step1")
				.setPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.addKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
				.build()).buildED()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		// Expected
		ArrayList<EntityData> expectedPayload = new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.addKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
						.build()).buildED();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(1, 0, 0, 1, expectedPayload));
		
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
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
	*/
	
    @Override
    protected String getComponentId() {
        return RdbmsReader.TYPE;
    }

	@Override
	public void setupHandle() {
		super.setupHandle();
		
		doReturn(this.sqls).when((RdbmsReader) spy).getSqls();
		
	}
	
	
}
