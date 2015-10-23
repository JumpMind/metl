package org.jumpmind.metl.core.runtime.component;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.utils.TestUtils;
import org.jumpmind.properties.TypedProperties;
import org.junit.Test;

public class XmlReaderTest extends AbstractComponentRuntimeTestSupport<ArrayList<String>> {

    @Override
    public void testStartDefaults() {
        // TODO Auto-generated method stub

    }

    @Override
    public void testStartWithValues() {
        // TODO Auto-generated method stub

    }

    @Test
    @Override
    public void testHandleStartupMessage() {
        setInputMessage(new ControlMessage());
        runHandle();
        assertHandle(0, getExpectedMessageMonitor(0, 0));
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

        Message message1 = new MessageBuilder("step1").withPayloadString(
                new PayloadBuilder().addRow("/Users/joshhicks/Documents/clients/Ascena/work/RMS/mom-4175/ItemExport_4175.xml").buildString())
                .build();

        messages.clear();
        messages.add(new HandleParams(message1, true));

        // Expected
        Message expectedMessage = new MessageBuilder()
                .withPayloadString(new PayloadBuilder().addRow(TestUtils.getTestXMLFileContent(TestUtils.XML_BASIC)).buildString()).build();

        List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
        expectedMonitors.add(getExpectedMessageMonitor(true, expectedMessage));

        runHandle();
        assertHandle(1, expectedMonitors);
    }

    @Test
    public void testHandleForReadTag() {
        // Setup
        setupHandle(TestUtils.XML_BASIC);
        ((XmlReader) spy).getFileNameFromMessage = true;
        ((XmlReader) spy).readTag = "Header";

        Message message1 = new MessageBuilder("step1").withPayloadString(
                new PayloadBuilder().addRow("/Users/joshhicks/Documents/clients/Ascena/work/RMS/mom-4175/ItemExport_4175.xml").buildString())
                .build();

        messages.clear();
        messages.add(new HandleParams(message1, true));

        // Expected
        Message expectedMessage = new MessageBuilder()
                .withPayloadString(new PayloadBuilder().addRow("<Header>" + "<Title>Title</Title>" + "</Header>").buildString()).build();

        List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
        expectedMonitors.add(getExpectedMessageMonitor(true, expectedMessage));

        runHandle();
        assertHandle(1, expectedMonitors);
    }

    @Test
    public void testHandleForReadTagMultiple() {
        // Setup
        setupHandle(TestUtils.XML_BASIC);
        ((XmlReader) spy).getFileNameFromMessage = true;
        ((XmlReader) spy).readTag = "Item";

        Message message1 = new MessageBuilder("step1").withPayloadString(
                new PayloadBuilder().addRow("/Users/joshhicks/Documents/clients/Ascena/work/RMS/mom-4175/ItemExport_4175.xml").buildString())
                .build();

        messages.clear();
        messages.add(new HandleParams(message1, true));

        // Expected
        Message expectedMessage1 = new MessageBuilder().withPayloadString(
                new PayloadBuilder().addRow("<Item>" + "<Id>1</Id>" + "<Name>Hat</Name>" + "<Size>Large</Size>" + "</Item>").buildString())
                .build();

        Message expectedMessage2 = new MessageBuilder().withPayloadString(
                new PayloadBuilder().addRow("<Item>" + "<Id>2</Id>" + "<Name>Shirt</Name>" + "<Size>Medium</Size>" + "</Item>").buildString())
                .build();

        List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
        expectedMonitors.add(getExpectedMessageMonitor(true, expectedMessage1, expectedMessage2));

        runHandle();
        assertHandle(2, expectedMonitors);
    }

    @Test
    public void testHandleForSingleLineXML() {
        // Setup
        setupHandle(TestUtils.XML_SINGLE_LINE);
        ((XmlReader) spy).getFileNameFromMessage = true;

        Message message1 = new MessageBuilder("step1").withPayloadString(
                new PayloadBuilder().addRow("/Users/joshhicks/Documents/clients/Ascena/work/RMS/mom-4175/ItemExport_4175.xml").buildString())
                .build();

        messages.clear();
        messages.add(new HandleParams(message1, true));

        // Expected
        Message expectedMessage1 = new MessageBuilder()
                .withPayloadString(new PayloadBuilder().addRow(TestUtils.getTestXMLFileContent(TestUtils.XML_SINGLE_LINE)).buildString())
                .build();

        List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
        expectedMonitors.add(getExpectedMessageMonitor(true, expectedMessage1));

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

        Resource resource = new Resource();
        when(component.getResource()).thenReturn(resource);
        when(component.getResourceId()).thenReturn(resource.getId());
        Map<String, IResourceRuntime> deployedResources = new HashMap<>();
        deployedResources.put(component.getResourceId(), mockResourceRuntime);
        when(context.getDeployedResources()).thenReturn(deployedResources);
        
        File xmlFile = TestUtils.getTestXMLFile(xmlFileName);

        when(mockResourceRuntime.getResourceRuntimeSettings()).thenReturn(mockTypedProperties);

        when(mockTypedProperties.get(anyString())).thenReturn("localFilePath");

        when(((XmlReader) spy).getFile(anyString())).thenReturn(xmlFile);
        when(((XmlReader) spy).getFile(anyString(), anyString())).thenReturn(xmlFile);

    }
}
