/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
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
    	MessageTestHelper.addControlMessage(this, "test", false);
		runHandle();
		assertHandle(0);
    }

    @Override
    public void testHandleUnitOfWorkLastMessage() {
    	setupHandle();
		
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		runHandle();
		assertHandle(0);
    }

    @Test
    @Override
    public void testHandleNormal() {
        // Setup
        setupHandle(TestUtils.XML_BASIC);
        ((XmlReader) spy).getFileNameFromMessage = true;

        MessageTestHelper.addInputMessage(this, true, true, "step1", "/text.xml");

        // Expected
        MessageTestHelper.addOutputMonitor(this, TestUtils.getTestXMLFileContent(TestUtils.XML_BASIC));
        runHandle();
        assertHandle(1);
    }

    @Test
    public void testHandleForReadTag() {
        // Setup
        setupHandle(TestUtils.XML_BASIC);
        ((XmlReader) spy).getFileNameFromMessage = true;
        ((XmlReader) spy).readTag = "Header";

        MessageTestHelper.addInputMessage(this, true, true, "step1", "/text.xml");

        // Expected
        MessageTestHelper.addOutputMonitor(this, true, "<Header>" + "<Title>Title</Title>" + "</Header>");

        runHandle();
        assertHandle(1);
    }

    @Test
    public void testHandleForReadTagMultiple() {
        // Setup
        setupHandle(TestUtils.XML_BASIC);
        ((XmlReader) spy).getFileNameFromMessage = true;
        ((XmlReader) spy).readTag = "Item";

        MessageTestHelper.addInputMessage(this, true, true, "step1", "/text.xml");

        // Expected
        MessageTestHelper.addOutputMonitor(this, true,
        		new MessageBuilder().withValue("<Item>" + "<Id>1</Id>" + "<Name>Hat</Name>" + "<Size>Large</Size>" + "</Item>").build(),
        		new MessageBuilder().withValue("<Item>" + "<Id>2</Id>" + "<Name>Shirt</Name>" + "<Size>Medium</Size>" + "</Item>").build());

        runHandle();
        assertHandle(2);
    }

    @Test
    public void testHandleForSingleLineXML() {
        // Setup
        setupHandle(TestUtils.XML_SINGLE_LINE);
        ((XmlReader) spy).getFileNameFromMessage = true;

        MessageTestHelper.addInputMessage(this, true, true, "step1", "/text.xml");

        // Expected
        MessageTestHelper.addOutputMonitor(this, true, TestUtils.getTestXMLFileContent(TestUtils.XML_SINGLE_LINE));

        runHandle();
        assertHandle(1);
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
