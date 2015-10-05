package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.properties.TypedProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class FilePollerTest extends AbstractComponentRuntimeTest<ArrayList<String>> {

	@Test
	@Override
	public void testHandleStartupMessage() {
		setupHandle(true);
		setInputMessage(new StartupMessage());
		runHandle();
		assertHandle(1, getExpectedMessageMonitorSingle(1, 0, 0, 1));
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle(true);
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(1, getExpectedMessageMonitorSingle(1, 0, 0, 1));
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle(true);
		
		// Expected
		ArrayList<String> expectedPayload = new PayloadBuilder()
						.addRow("fileAbsolutePath").buildString();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(1, 0, 0, 1, expectedPayload));
				
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}
	
	@Test
	public void testHandleCancelOnShutdown() {
		// Setup
		setupHandle(true);
		
		((FilePoller) spy).cancelOnNoFiles = true;
		DirectoryScanner mockDirectoryScanner = mock(DirectoryScanner.class);
		when(((FilePoller) spy).getDirectoryScanner()).thenReturn(mockDirectoryScanner);
		when(mockDirectoryScanner.getIncludedFiles()).thenReturn(new String[] {});
		
		// Expected
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(0, 0, 1, 0));
				
		// Execute and Assert
		runHandle();
		assertHandle(0, expectedMonitors);
	}
	
	@Test
	public void testHandleUseTriggerFile() {
		// Setup
		setupHandle(true);
		
		((FilePoller) spy).useTriggerFile = true;
		
		// Expected
		ArrayList<String> expectedPayload = new PayloadBuilder()
				.addRow("fileAbsolutePath").buildString();

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(1, 0, 0, 1, expectedPayload));
				
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}
	
	@Test
	public void testHandleCancelOnShutdownWithTriggerFile() {
		// Setup
		setupHandle(false);
		
		((FilePoller) spy).cancelOnNoFiles = true;
		((FilePoller) spy).useTriggerFile = true;
		
		DirectoryScanner mockDirectoryScanner = mock(DirectoryScanner.class);
		when(((FilePoller) spy).getDirectoryScanner()).thenReturn(mockDirectoryScanner);
		when(mockDirectoryScanner.getIncludedFiles()).thenReturn(new String[] {});
		
		// Expected
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(0, 0, 1, 0));
				
		// Execute and Assert
		runHandle();
		assertHandle(0, expectedMonitors);
	}

	@Override
	protected String getComponentId() {
		return FilePoller.TYPE;
	}
	
	public void setupHandle(boolean fileExists) {
		super.setupHandle();
		
		((FilePoller) spy).maxFilesToPoll = 5;
		
		IResourceRuntime mockResourceRuntime = mock(IResourceRuntime.class);
		TypedProperties mockTypedProperties = mock(TypedProperties.class);
		File mockFile = mock(File.class);
		DirectoryScanner mockDirectoryScanner = mock(DirectoryScanner.class);
		
		when(((FilePoller) spy).getResourceRuntime()).thenReturn(mockResourceRuntime);
		when(mockResourceRuntime.getResourceRuntimeSettings()).thenReturn(mockTypedProperties);
		when(mockTypedProperties.get(anyString())).thenReturn("localFilePath");
		
		when(((FilePoller) spy).getNewFile(anyString())).thenReturn(mockFile);
		when(((FilePoller) spy).getNewFile(anyString(), anyString())).thenReturn(mockFile);
		when(((FilePoller) spy).getDirectoryScanner()).thenReturn(mockDirectoryScanner);
		when(mockDirectoryScanner.getIncludedFiles()).thenReturn(new String[] {"file"});
		when(mockFile.getAbsolutePath()).thenReturn("fileAbsolutePath");
		when(mockFile.lastModified()).thenReturn(1L);
		when(mockFile.exists()).thenReturn(fileExists);
	}

}

