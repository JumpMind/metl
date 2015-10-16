package org.jumpmind.metl.core.runtime.component;

import java.util.ArrayList;

import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TextConstantTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setupHandle();
		setInputMessage(new ControlMessage());
		((TextConstant) spy).constantText = "";
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow(new String("")).buildString()).build();
				
		runHandle();
		assertHandle(1, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		((TextConstant) spy).constantText = "";
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow(new String("")).buildString()).build();
				
		runHandle();
		assertHandle(1, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO BUCKS";
		((TextConstant) spy).splitOnLineFeed = false;
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Ohio State").buildString()).build();
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow("GO BUCKS").buildString()).build();
				
		runHandle();
		assertHandle(1, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}
	
	@Test
	public void testHandleSplitOnLineFeedNoLineFeed() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO BUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 2;
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Ohio State\nBuckeyes").buildString()).build();
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder().addRow("GO BUCKS").buildString()).build();
				
		runHandle();
		assertHandle(1, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}
	
	@Test
	public void testHandleSplitOnLineFeedWithLineFeed() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO\nBUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 2;
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Ohio State\nBuckeyes").buildString()).build();
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
					.addRow("GO")
					.addRow("BUCKS").buildString()).build();
				
		runHandle();
		assertHandle(2, getExpectedMessageMonitor(0, 0, false, expectedMessage));
	}
	
	@Test
	public void testHandleSplitOnLineFeedWithLineFeedAndRowsExceeded() {
		// Setup
		setupHandle();
		((TextConstant) spy).constantText = "GO\nBUCKS";
		((TextConstant) spy).splitOnLineFeed = true;
		((TextConstant) spy).textRowsPerMessage = 1;
		
		Message message1 = new MessageBuilder("step1")
				.withPayloadString(new PayloadBuilder()
						.addRow("Ohio State\nBuckeyes").buildString()).build();
		
		// Expected
		Message expectedMessage1 = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
					.addRow("GO")
					.buildString()).build();
				
		Message expectedMessage2 = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
					.addRow("BUCKS").buildString()).build();
		
		runHandle();
		assertHandle(2, getExpectedMessageMonitor(0, 0, false, expectedMessage1, expectedMessage2));
	}

	@Override
	protected String getComponentId() {
		return TextConstant.TYPE;
	}
	
	

}
