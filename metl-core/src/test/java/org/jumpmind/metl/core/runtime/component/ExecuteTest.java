package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Mockito.mock;

import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelAttributeScriptHelper.class)
public class ExecuteTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>> {

	public static final String COMMAND_OUTPUT = "command output";
	
	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		try {
			((Execute) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
				.withSetting(Execute.COMMAND, "top")
				.withSetting(Execute.CONTINUE_ON_ERROR, "true")
				.withSetting(Execute.SUCCESS_CODE, "1").build());
		
		((Execute) spy).start();
		
		String[] actual = ((Execute) spy).commands;
		String[] expected = new String[] { "top" };
		
		Assert.assertArrayEquals(expected, actual);
		Assert.assertTrue(((Execute) spy).continueOnError);
		Assert.assertEquals(1, ((Execute) spy).successCode);
	}
	
	@Test
	public void testStartDetaultsWithCommand() {
		setupStart(new SettingsBuilder()
				.withSetting(Execute.COMMAND, "top").build());
		((Execute) spy).start();
		
		Assert.assertFalse(((Execute) spy).continueOnError);
		Assert.assertEquals(0, ((Execute) spy).successCode);
	}
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		setupHandle(0);
		setInputMessage(new ControlMessage());
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
				.addRow(COMMAND_OUTPUT).buildString()).build();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
				
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(expectedMessage));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle(0);
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
				.addRow(COMMAND_OUTPUT).buildString()).build();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
				
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(expectedMessage));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle(0);
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
				.addRow(COMMAND_OUTPUT).buildString()).build();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
		runHandle();
		assertHandle(0, expectedMonitors);
	}
	
	@Test
	public void testHandleError() {
		// Setup
		setupHandle(1);
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
				.addRow(COMMAND_OUTPUT).buildString()).build();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
		try {
			runHandle();
			Assert.fail("Should of thrown IOException");
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IoException);
		}
	}
	
	@Test
	public void testHandleErrorContinue() {
		// Setup
		setupHandle(1);
		((Execute) spy).continueOnError = true;
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
				.addRow(COMMAND_OUTPUT).buildString()).build();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
		runHandle();
		assertHandle(0, expectedMonitors);
		
	}

	@Override
	protected String getComponentId() {
		return Execute.TYPE;
	}
	
	public void setupHandle(int outputError) {
		super.setupHandle();
		
		org.apache.tools.ant.taskdefs.Execute mockExecute = mock(org.apache.tools.ant.taskdefs.Execute.class);
		ByteArrayOutputStream mockOutputStream = mock(ByteArrayOutputStream.class);
		
		try {
			when(((Execute) spy).getAntTask(Mockito.any(PumpStreamHandler.class))).thenReturn(mockExecute);
			when(((Execute) spy).getByteArrayOutputStream()).thenReturn(mockOutputStream);
			when(mockExecute.execute()).thenReturn(outputError);
			when(mockOutputStream.toByteArray()).thenReturn(COMMAND_OUTPUT.getBytes());
		}
		catch (Exception e) {
			Assert.fail("Unable to execute " + e.getMessage());
		}
	}

}
