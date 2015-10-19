package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ModelAttributeBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class SorterTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		try {
			((Sorter) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	public void testStartIncorrectEntityAttribute() {
		properties.put(Sorter.SORT_ATTRIBUTE, MODEL_ATTR_ID_1);
		
		setupStart(new SettingsBuilder().build());
		try {
			((Sorter) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}
	
	@Test
	public void testStartAttributeMissingFromModel() {
		setupStart(new SettingsBuilder().build());
				
		properties.put(Sorter.SORT_ATTRIBUTE, "ENTITY1." + MODEL_ATTR_ID_1);
		
		Mockito.when(inputModel.getAttributeByName("ENTITY1", MODEL_ATTR_ID_1))
			.thenReturn(new ModelAttributeBuilder().withId(MODEL_ATTR_ID_1).build());
		
		((Sorter) spy).start();
		Assert.assertEquals(MODEL_ATTR_ID_1, ((Sorter) spy).sortAttributeId);
	}

	
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
			.withSetting(Sorter.SORT_ATTRIBUTE, MODEL_ATTR_ID_1)
			.withSetting(Sorter.ROWS_PER_MESSAGE, "5").build());
		((Sorter) spy).start();
		
		
	}
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		setupHandle();
		setInputMessage(new ControlMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle();
		((Sorter) spy).sortAttributeId = MODEL_ATTR_ID_1;
		
		Message message1 = new MessageBuilder("step1")
				.withPayload(new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, "superman")
					.build()).buildED()).build();
		
		Message message2 = new MessageBuilder("step2")
				.withPayload(new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, "iron man")
					.build()).buildED()).build();
		
		Message message3 = new MessageBuilder("step2")
				.withPayload(new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, "flash")
					.build()).buildED()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, false));
		messages.add(new HandleParams(message2, false));
		messages.add(new HandleParams(message3, true));
		
		// Expected
		Message expectedMessage1 = new MessageBuilder().withPayload(
				new PayloadBuilder().buildED()).build();
		
		Message expectedMessage2 = new MessageBuilder().withPayload(new PayloadBuilder()
				.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, "flash")
					.build()).buildED()).build();
		
		Message expectedMessage3 = new MessageBuilder().withPayload(new PayloadBuilder()
				.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, "iron man")
					.build()).buildED()).build();
		
		Message expectedMessage4 = new MessageBuilder().withPayload(new PayloadBuilder()
				.addRow(new EntityDataBuilder()
					.withKV(MODEL_ATTR_ID_1, "superman")
				.build()).buildED()).build();

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();		
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, false, null));
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, false, null));
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, false, 
				expectedMessage1, expectedMessage2, expectedMessage3, expectedMessage4));
		
		runHandle();
		assertHandle(3, expectedMonitors);
		
	}

	@Override
	protected String getComponentId() {
		return Sorter.TYPE;
	}

}
