package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.component.DelimitedParser.AttributeFormat;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ModelEntityBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ModelHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelAttributeScriptHelper.class)
public class DelimitedParserTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new ControlMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(1, 0, 0, 1));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(1, 0, 0, 1));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle();
		
		setUnitOfWorkLastMessage(true);
		
		Message message1 = new MessageBuilder("step1")
				.setPayloadString(new PayloadBuilder()
					.addRow("red,yellow,green,blue,purple,orange")
					.buildString()).build();
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		Model model = ModelHelper.createSimpleModel(2, 3);
		this.outputModel = model;
		when(component.getOutputModel()).thenReturn(model);
		
		((DelimitedParser) spy).attributes = new ArrayList<AttributeFormat>();
		AttributeFormat mAttr = ((DelimitedParser) spy).new AttributeFormat(
				ModelHelper.ATTR_ID + "0_0", null, null);
		mAttr.setOrdinal(1);
		((DelimitedParser) spy).attributes.add(mAttr);
		
		// Expected
		ArrayList<EntityData> expectedPayload = new PayloadBuilder()
				.addRow(new EntityDataBuilder()
					.withKV(ModelHelper.ATTR_ID + "0_0", "red")
				.build()).buildED();
				

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(1, 0, 0, 1, expectedPayload));
				
		runHandle();
		assertHandle(1, expectedMonitors);
	}
	
	@Test
	public void testHandleWithScriptEval() {
		// Setup
		setupHandle();
		
		PowerMockito.mockStatic(ModelAttributeScriptHelper.class);
		try {
			PowerMockito.doReturn("maroon").when(
				ModelAttributeScriptHelper.class, 
				"eval",
				Mockito.any(ModelAttribute.class), 
				Mockito.anyObject(), 
				Mockito.any(ModelEntity.class), 
				Mockito.any(EntityData.class), 
				Mockito.anyString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		setUnitOfWorkLastMessage(true);
		
		Message message1 = new MessageBuilder("step1")
				.setPayloadString(new PayloadBuilder()
					.addRow("red,yellow,green,blue,purple,orange")
					.buildString()).build();
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		Model model = ModelHelper.createSimpleModel(2, 3);
		when(component.getOutputModel()).thenReturn(model);
		
		((DelimitedParser) spy).attributes = new ArrayList<AttributeFormat>();
		AttributeFormat mAttr = ((DelimitedParser) spy).new AttributeFormat(
				ModelHelper.ATTR_ID + "0_0", null, null);
		mAttr.setOrdinal(1);
		mAttr.setFormatFunction("someFunction");
		((DelimitedParser) spy).attributes.add(mAttr);
		
		// Expected
		ArrayList<EntityData> expectedPayload = new PayloadBuilder()
				.addRow(new EntityDataBuilder()
					.withKV(ModelHelper.ATTR_ID + "0_0", "maroon")
				.build()).buildED();
				

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(1, 0, 0, 1, expectedPayload));
				
		runHandle();
		assertHandle(1, expectedMonitors);
	}
	
	@Test
	public void testHandleWithoutAttributesSet() {
		// Setup
		setupHandle();
		
		setUnitOfWorkLastMessage(true);
		
		Message message1 = new MessageBuilder("step1")
				.setPayloadString(new PayloadBuilder()
					.addRow("red,yellow,green,blue,purple,orange")
					.buildString()).build();
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		Model model = ModelHelper.createSimpleModel(2, 3);
		
		this.outputModel = model;
		when(component.getOutputModel()).thenReturn(model);
		
		
		// Expected
		ArrayList<EntityData> expectedPayload = new PayloadBuilder()
				.addRow(new EntityDataBuilder()
					.withKV(ModelHelper.ATTR_ID + "0-0", "red")
					.withKV(ModelHelper.ATTR_ID + "1-0", "yellow")
					.withKV(ModelHelper.ATTR_ID + "2-0", "green")
					.withKV(ModelHelper.ATTR_ID + "0-1", "blue")
					.withKV(ModelHelper.ATTR_ID + "1-1", "purple")
					.withKV(ModelHelper.ATTR_ID + "2-1", "orange")
				.build()).buildED();
				

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(1, 0, 0, 1, expectedPayload));
			
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}
	
	@Test
	public void testHandleSkipHeaderAndFooterRows() {
		// Setup
		setupHandle();
		
		setUnitOfWorkLastMessage(true);
		
		Message message1 = new MessageBuilder("step1")
				.setPayloadString(new PayloadBuilder()
					.addRow("h1,h2,h3,h4,h5,h6")
					.addRow("red,yellow,green,blue,purple,orange")
					.addRow("f1,f2,f3,f4,f5,f6")
					.buildString()).build();
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		((DelimitedParser) spy).numberOfHeaderLinesToSkip = 1;
		((DelimitedParser) spy).numberOfFooterLinesToSkip = 1;
		
		Model model = ModelHelper.createSimpleModel(2, 3);
		when(component.getOutputModel()).thenReturn(model);
		
		
		// Expected
		ArrayList<EntityData> expectedPayload = new PayloadBuilder()
				.addRow(new EntityDataBuilder()
					.withKV(ModelHelper.ATTR_ID + "0-0", "red")
					.withKV(ModelHelper.ATTR_ID + "1-0", "yellow")
					.withKV(ModelHelper.ATTR_ID + "2-0", "green")
					.withKV(ModelHelper.ATTR_ID + "0-1", "blue")
					.withKV(ModelHelper.ATTR_ID + "1-1", "purple")
					.withKV(ModelHelper.ATTR_ID + "2-1", "orange")
				.build()).buildED();
				

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(1, 0, 0, 1, expectedPayload));
			
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}


	@Override
	public IComponentRuntime getComponentSpy() {
		return spy(new DelimitedParser());
	}

	@Override
	protected String getComponentId() {
		return DelimitedParser.TYPE;
	}

}
