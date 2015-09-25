package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MessageManipulationStrategy;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.flow.IMessageTarget;
import org.jumpmind.properties.TypedProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class RdmsReaderUnitTest extends AbstractDbComponentTest {

	@Test
	public void testApplySettings() {
		// setup
		RdbmsReader reader = spy(new RdbmsReader());
		
		String eSql = "select * from test";
		long eRowsPerMessage = 5l;
		String eMessageManipulationStrategy = MessageManipulationStrategy.ENHANCE.name();
		String eTrimColumns = "true";
		String eMatchOnColumnNameOnly = "true";
		
		TypedProperties eProperties = new TypedProperties();
		eProperties.setProperty(reader.SQL, eSql);
		eProperties.setProperty(reader.ROWS_PER_MESSAGE, eRowsPerMessage);
		eProperties.setProperty(reader.MESSAGE_MANIPULATION_STRATEGY, eMessageManipulationStrategy);
		eProperties.setProperty(reader.TRIM_COLUMNS, eTrimColumns);
		eProperties.setProperty(reader.MATCH_ON_COLUMN_NAME_ONLY, eMatchOnColumnNameOnly);
				
		doReturn(eProperties).when(reader).getTypedProperties();
		
		// actual
		reader.applySettings();
		assertTrue(reader.isMatchOnColumnNameOnly());
		assertTrue(reader.isTrimColumns());
		assertEquals(eRowsPerMessage, reader.getRowsPerMessage());
		assertEquals(1, reader.getSqls().size());
		assertEquals(eSql, reader.getSqls().get(0));
		assertEquals(MessageManipulationStrategy.ENHANCE, reader.getMessageManipulationStrategy());
	}
	
	@Test
	public void testHandleStartupNoResults() {
		// setup
		Message resultMessage = new Message("");
		MessageTarget target = new MessageTarget();
		RdbmsReaderComponentSettings settings = new RdbmsReaderComponentSettings();
				
		// execute
		RdbmsReader reader = runHandle(new StartupMessage(), resultMessage, target, settings);
		
		// verify
		assertEquals(0, target.getTargetMessageCount());
		assertEquals(1, reader.getComponentStatistics().getNumberInboundMessages());
	}
	
	@Test
	public void testHandleStartupWithResults() {
		// setup
		Message resultMessage = new Message("");
		MessageTarget target = new MessageTarget();
		RdbmsReaderComponentSettings settings = new RdbmsReaderComponentSettings();
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from test");
		settings.setSqls(sqls);
		
		// execute
		RdbmsReader reader = runHandle(new StartupMessage(), resultMessage, target, settings);
		
		// verify
		assertEquals(1, target.getTargetMessageCount());
		assertTrue(target.getMessage(0).getHeader().isLastMessage());
		assertEquals(1, reader.getComponentStatistics().getNumberInboundMessages());
	}
	
	@Test
	public void testHandleWithFlowParameters() {
		// setup
		Message inputMessage = new Message("test");
		Message resultMessage = new Message("");
		MessageTarget target = new MessageTarget();
		RdbmsReaderComponentSettings settings = new RdbmsReaderComponentSettings();
		
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from $(UNIT_TEST)");
		
		Map<String, String> flowParameters = new HashMap<String, String>();
		flowParameters.put("UNIT_TEST", "GOES_BOOM");
		
		settings.setSqls(sqls);
		settings.setFlowParametersAsString(flowParameters);
		settings.setExpectedFlowReplacementSql("select * from GOES_BOOM");
		
		// execute
		RdbmsReader reader = runHandle(inputMessage, resultMessage, target, settings);
		
		// verify
		assertEquals(1, target.getTargetMessageCount());
		assertTrue(target.getMessage(0).getHeader().isLastMessage());
		assertEquals(1, reader.getComponentStatistics().getNumberInboundMessages());
	}
	
	@Test
	public void testHandleSettingParamsFromHeaderNoPayload() {
		Message inputMessage = new Message("test");
		Message resultMessage = new Message("");
		MessageTarget target = new MessageTarget();
		
		RdbmsReaderComponentSettings settings = new RdbmsReaderComponentSettings();

		Map<String, Serializable> flowParameters = new HashMap<String, Serializable>();
		flowParameters.put("key", "value");
		
		List<String> sqls = new ArrayList<String>();
		sqls.add("select * from test");
		
		Map<String, Object> expectedParams = new HashMap<String, Object>();
		expectedParams.putAll(flowParameters);
		
		settings.setSqls(sqls);
		settings.setFlowParameters(flowParameters);
		settings.setExpectedParamMap(expectedParams);
		// execute
		RdbmsReader reader = runHandle(inputMessage, resultMessage, target, settings);
		
		// verify
		assertEquals(1, reader.getComponentStatistics().getNumberInboundMessages());
		
	}
	
	@Test
	public void testHandleSettingParamsFromHeaderWithPayload() {
		// TODO
	}
	private RdbmsReader runHandle(Message inputMessage, Message resultMessage, MessageTarget target, 
			RdbmsReaderComponentSettings settings) {
		
		RdbmsReader sReader = spy(new RdbmsReader());
		doReturn(settings.getSqls()).when(sReader).getSqls();
		doNothing().when(sReader).start();
		
		super.setupHandle(sReader, inputMessage, resultMessage, settings);
		
		sReader.handle(inputMessage, target);
		return sReader;
	}
	
	class MessageTarget implements IMessageTarget {

        List<Message> targetMsgArray = new ArrayList<Message>();

        @Override
        public void put(Message message) {
            targetMsgArray.add(message);
        }

        public Message getMessage(int idx) {
            return targetMsgArray.get(idx);
        }

        public int getTargetMessageCount() {
            return targetMsgArray.size();
        }
    }
	
}
