package org.jumpmind.metl.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TextReplaceTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>> {
	

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder()
				.withSetting(TextReplace.SETTING_SEARCH_FOR, "")
				.withSetting(TextReplace.SETTING_REPLACE_WITH, "").build());
		
		try {
			((TextReplace) spy).start();
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}
		
		assertEquals("", ((TextReplace) spy).searchFor);
		assertEquals("", ((TextReplace) spy).replaceWith);
	}
	
	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
				.withSetting(TextReplace.SETTING_SEARCH_FOR, "search")
				.withSetting(TextReplace.SETTING_REPLACE_WITH, "replace").build());
		 
		((TextReplace) spy).start();
		
		assertEquals("search", ((TextReplace) spy).searchFor);
		assertEquals("replace", ((TextReplace) spy).replaceWith);
	}
	
	@Test
	public void testStartWithMissingProperties() {
		setupStart(new SettingsBuilder()
			.withSetting(TextReplace.SETTING_SEARCH_FOR, null)
			.withSetting(TextReplace.SETTING_REPLACE_WITH, "replace").build());
				
		try {
			((TextReplace) spy).start();
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}
	}
	
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
