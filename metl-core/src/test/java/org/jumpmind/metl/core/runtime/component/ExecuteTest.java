package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
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
	public void testHandleStartupMessage() {
		setupHandle(0);
		setInputMessage(new StartupMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(1, 0, 0, 1));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle(0);
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(1, 0, 0, 1));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle(0);
		
		// Expected
		ArrayList<String> expectedPayload = new PayloadBuilder()
				.addRow(COMMAND_OUTPUT).buildString();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(1, 0, 0, 1, expectedPayload));
				
		runHandle();
		assertHandle(0, expectedMonitors);
	}
	
	@Test
	public void testHandleError() {
		// Setup
		setupHandle(1);
		
		// Expected
		ArrayList<String> expectedPayload = new PayloadBuilder()
				.addRow(COMMAND_OUTPUT).buildString();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(1, 0, 0, 1, expectedPayload));
				
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
		ArrayList<String> expectedPayload = new PayloadBuilder()
				.addRow(COMMAND_OUTPUT).buildString();
				
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(1, 0, 0, 1, expectedPayload));
				
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
