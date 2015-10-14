package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.utils.TestUtils;
import org.jumpmind.properties.TypedProperties;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class XmlReaderTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>>{

	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new StartupMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(0, 0, 0, 0));
	}

	@Override
	public void testHandleUnitOfWorkLastMessage() {
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle(TestUtils.XML_BASIC);
		((XmlReader) spy).getFileNameFromMessage = true;
		
		Message message1 = new MessageBuilder("step1")
				.setPayloadString(new PayloadBuilder()
					.addRow("/Users/joshhicks/Documents/clients/Ascena/work/RMS/mom-4175/ItemExport_4175.xml")
					.buildString()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		// Expected
		ArrayList<String> expectedPayload = new PayloadBuilder()
				.addRow(TestUtils.getTestXMLFileContent(TestUtils.XML_BASIC)).buildString();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(1, 0, 0, 1, expectedPayload, true));
						
		runHandle();
		assertHandle(1, expectedMonitors);
	}

	@Test
	public void testHandleForReadTag() {
		// Setup
		setupHandle(TestUtils.XML_BASIC);
		((XmlReader) spy).getFileNameFromMessage = true;
		((XmlReader) spy).readTag = "Header";
		
		Message message1 = new MessageBuilder("step1")
				.setPayloadString(new PayloadBuilder()
					.addRow("/Users/joshhicks/Documents/clients/Ascena/work/RMS/mom-4175/ItemExport_4175.xml")
					.buildString()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		// Expected
		ArrayList<String> expectedPayload = new PayloadBuilder()
				.addRow("<Header>" + 
							"<Title>Title</Title>" + 
						"</Header>").buildString();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(1, 0, 0, 1, expectedPayload, true));
						
		runHandle();
		assertHandle(1, expectedMonitors);
	}

	@Test
	public void testHandleForSingleLineXML() {
		// Setup
		setupHandle(TestUtils.XML_SINGLE_LINE);
		((XmlReader) spy).getFileNameFromMessage = true;
		
		Message message1 = new MessageBuilder("step1")
				.setPayloadString(new PayloadBuilder()
					.addRow("/Users/joshhicks/Documents/clients/Ascena/work/RMS/mom-4175/ItemExport_4175.xml")
					.buildString()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		// Expected
		ArrayList<String> expectedPayload = new PayloadBuilder()
				.addRow(TestUtils.getTestXMLFileContent(TestUtils.XML_SINGLE_LINE)).buildString();
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedTextMessageMonitor(1, 0, 0, 1, expectedPayload, true));
						
		runHandle();
		assertHandle(1, expectedMonitors);
	}

	
	
	@Override
	protected String getComponentId() {
		return XmlReader.TYPE;
	}

	public void setupHandle(String xmlFileName) {
		super.setupHandle();
		
		IResourceRuntime mockResourceRuntime = mock(IResourceRuntime.class);
		TypedProperties mockTypedProperties = mock(TypedProperties.class);
		
		File xmlFile = TestUtils.getTestXMLFile(xmlFileName);
		FileReader mockFileReader = mock(FileReader.class);
		
		when(((XmlReader) spy).getResourceRuntime()).thenReturn(mockResourceRuntime);
		when(mockResourceRuntime.getResourceRuntimeSettings()).thenReturn(mockTypedProperties);
		when(mockTypedProperties.get(anyString())).thenReturn("localFilePath");
		
		when(((XmlReader) spy).getFile(anyString())).thenReturn(xmlFile);
		when(((XmlReader) spy).getFile(anyString(), anyString())).thenReturn(xmlFile);
		
	}
}
