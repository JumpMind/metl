package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;

import org.apache.tools.ant.DirectoryScanner;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.properties.TypedProperties;
import org.junit.Test;

public class XmlReaderTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>>{

	@Override
	public void testHandleStartupMessage() {
	}

	@Override
	public void testHandleUnitOfWorkLastMessage() {
	}

	@Override
	public void testHandleNormal() {
		setupHandle();
		((XmlReader) spy).getFileNameFromMessage = true;
		
		Message message1 = new MessageBuilder("step1")
				.setPayloadString(new PayloadBuilder()
					.addRow("/Users/joshhicks/Documents/clients/Ascena/work/RMS/mom-4175/ItemExport_4175.xml")
					.buildString()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		runHandle();
	}

	@Override
	protected String getComponentId() {
		return XmlReader.TYPE;
	}

	public void setupHandle() {
		super.setupHandle();
		
		
		IResourceRuntime mockResourceRuntime = mock(IResourceRuntime.class);
		TypedProperties mockTypedProperties = mock(TypedProperties.class);
		
		when(((XmlReader) spy).getResourceRuntime()).thenReturn(mockResourceRuntime);
		when(mockResourceRuntime.getResourceRuntimeSettings()).thenReturn(mockTypedProperties);
		when(mockTypedProperties.get(anyString())).thenReturn("localFilePath");
		
	}
}
