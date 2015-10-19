package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.DirectoryScanner;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.properties.TypedProperties;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;



@RunWith(PowerMockRunner.class)
public class FilePollerTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>> {

	@Test
	@Override
	public void testStartDefaults() {
		Resource resource = mock(Resource.class);
		when(resource.getType()).thenReturn(LocalFile.TYPE);
		setupStart(new ComponentBuilder().withResource(resource).build());
		
		((FilePoller) spy).start();
		
		Assert.assertEquals(false,  ((FilePoller) spy).useTriggerFile);
		Assert.assertEquals(false,  ((FilePoller) spy).recurse);
		Assert.assertEquals(true,  ((FilePoller) spy).cancelOnNoFiles);
		Assert.assertEquals(FilePoller.ACTION_NONE,  ((FilePoller) spy).actionOnSuccess);
		Assert.assertEquals(FilePoller.ACTION_NONE,  ((FilePoller) spy).actionOnError);
		Assert.assertEquals(FilePoller.SORT_MODIFIED,  ((FilePoller) spy).fileSortOption);
	}
	
	@Test
	public void testStartDefaultsInvalidResourceType() {
		Resource resource = mock(Resource.class);
		when(resource.getType()).thenReturn("test");
		setupStart(new ComponentBuilder().withResource(resource).build());
		
		try {
			((FilePoller) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	@Override
	public void testStartWithValues() {
		Resource resource = mock(Resource.class);
		when(resource.getType()).thenReturn(LocalFile.TYPE);
		setupStart(new ComponentBuilder().withResource(resource).build());
		
		
		properties.put(FilePoller.SETTING_FILE_PATTERN, "*.jar");
		properties.put(FilePoller.SETTING_TRIGGER_FILE_PATH, "/trigger");
		properties.put(FilePoller.SETTING_USE_TRIGGER_FILE, "true");
		properties.put(FilePoller.SETTING_RECURSE, "true");
		properties.put(FilePoller.SETTING_CANCEL_ON_NO_FILES, "true");
		properties.put(FilePoller.SETTING_ACTION_ON_SUCCESS, FilePoller.ACTION_DELETE);
		properties.put(FilePoller.SETTING_ACTION_ON_ERROR, FilePoller.ACTION_DELETE);
		properties.put(FilePoller.SETTING_ARCHIVE_ON_ERROR_PATH, "/archive-fail");
		properties.put(FilePoller.SETTING_ARCHIVE_ON_SUCCESS_PATH, "/archive-success");
		properties.put(FilePoller.SETTING_MAX_FILES_TO_POLL, "10");
		properties.put(FilePoller.SETTING_FILE_SORT_ORDER, FilePoller.SORT_NAME);
		
		((FilePoller) spy).start();
		
		Assert.assertEquals("*.jar",  ((FilePoller) spy).filePattern);
		Assert.assertEquals("/trigger",  ((FilePoller) spy).triggerFilePath);
		Assert.assertEquals(true,  ((FilePoller) spy).useTriggerFile);
		Assert.assertEquals(true,  ((FilePoller) spy).recurse);
		Assert.assertEquals(true,  ((FilePoller) spy).cancelOnNoFiles);
		Assert.assertEquals(FilePoller.ACTION_DELETE,  ((FilePoller) spy).actionOnSuccess);
		Assert.assertEquals(FilePoller.ACTION_DELETE,  ((FilePoller) spy).actionOnError);
		Assert.assertEquals("/archive-fail",  ((FilePoller) spy).archiveOnErrorPath);
		Assert.assertEquals("/archive-success",  ((FilePoller) spy).archiveOnSuccessPath);
		Assert.assertEquals(10,  ((FilePoller) spy).maxFilesToPoll);
		Assert.assertEquals(FilePoller.SORT_NAME,  ((FilePoller) spy).fileSortOption);
	}
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		setupHandle(true);
		setInputMessage(new ControlMessage());
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
						.addRow("fileAbsolutePath").buildString()).build();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}

	@Test
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle(true);
		setUnitOfWorkLastMessage(true);
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
						.addRow("fileAbsolutePath").buildString()).build();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle(true);
		
		// Expected
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
						.addRow("fileAbsolutePath").buildString()).build();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
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
		expectedMonitors.add(getExpectedMessageMonitor(0, 1));
				
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
		Message expectedMessage = new MessageBuilder().withPayloadString(
				new PayloadBuilder()
				.addRow("fileAbsolutePath").buildString()).build();

		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(expectedMessage));
				
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
		expectedMonitors.add(getExpectedMessageMonitor(0,1));
				
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

