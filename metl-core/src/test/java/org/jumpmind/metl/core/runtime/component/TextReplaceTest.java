package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TextReplaceTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setupHandle();
		setInputMessage(new ControlMessage());
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().buildString()).build();
				
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().buildString()).build();
				
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle();
		((TextReplace) spy).searchFor = "replaceMe";
		((TextReplace) spy).replaceWith = "replaced";
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Someone please replaceMe").buildString()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, false));
		
		// Expected
		Message expectedMessage1 = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow("Someone please replaced").buildString())
				.build();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();		
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, false, expectedMessage1));
		
		runHandle();
		assertHandle(1, expectedMonitors);
	}
	
	@Test
	public void testHandleNoMatches() {
		// Setup
		setupHandle();
		((TextReplace) spy).searchFor = "replaceMe";
		((TextReplace) spy).replaceWith = "replaced";
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Someone please replace me").buildString()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, false));
		
		// Expected
		Message expectedMessage1 = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow("Someone please replace me").buildString())
				.build();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();		
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, false, expectedMessage1));
		
		runHandle();
		assertHandle(1, expectedMonitors);
	}

	@Override
	protected String getComponentId() {
		return TextReplace.TYPE;
	}
}
