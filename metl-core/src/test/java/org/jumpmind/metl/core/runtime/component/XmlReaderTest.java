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

import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.metl.core.runtime.resource.ResourceFactory;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Test;

public class XmlReaderTest extends AbstractComponentRuntimeTestSupport {

    @Override
    public void testStartDefaults() {
    }

    @Override
    public void testStartWithValues() {
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
    public void testHandleNormal() throws Exception {
        // Setup
        setupHandle(TestUtils.XML_BASIC);
        ((XmlReader) spy).getFileNameFromMessage = true;

        MessageTestHelper.addInputMessage(this, true, "step1", TestUtils.XML_BASIC);

        // Expected
        MessageTestHelper.addOutputMonitor(this, TestUtils.getTestXMLFileContent(TestUtils.XML_BASIC));
        runHandle();
        assertHandle(1);
    }

    @Test
    public void testHandleForReadTag() throws Exception {
        // Setup
        setupHandle(TestUtils.XML_BASIC);
        ((XmlReader) spy).getFileNameFromMessage = true;
        ((XmlReader) spy).readTag = "Header";

        MessageTestHelper.addInputMessage(this, true, "step1", TestUtils.XML_BASIC);

        // Expected
        MessageTestHelper.addOutputMonitor(this, true, "<Header>" + "<Title>Title</Title>" + "</Header>");

        runHandle();
        assertHandle(1);
    }

    @Test
    public void testHandleForReadTagMultiple() throws Exception {
        // Setup
        setupHandle(TestUtils.XML_BASIC);
        ((XmlReader) spy).getFileNameFromMessage = true;
        ((XmlReader) spy).readTag = "Item";

        MessageTestHelper.addInputMessage(this, true, "step1", TestUtils.XML_BASIC);

        // Expected
        MessageTestHelper.addOutputMonitor(this, true,
        		new MessageBuilder().withValue("<Item>" + "<Id>1</Id>" + "<Name>Hat</Name>" + "<Size>Large</Size>" + "</Item>").build(),
        		new MessageBuilder().withValue("<Item>" + "<Id>2</Id>" + "<Name>Shirt</Name>" + "<Size>Medium</Size>" + "</Item>").build());

        runHandle();
        assertHandle(2);
    }

    @Test
    public void testHandleForSingleLineXML() throws Exception {
        // Setup
        setupHandle(TestUtils.XML_SINGLE_LINE);
        ((XmlReader) spy).getFileNameFromMessage = true;

        MessageTestHelper.addInputMessage(this, true, "step1", TestUtils.XML_SINGLE_LINE);

        // Expected
        MessageTestHelper.addOutputMonitor(this, true, TestUtils.getTestXMLFileContent(TestUtils.XML_SINGLE_LINE));

        runHandle();
        assertHandle(1);
    }

    @Override
    protected String getComponentId() {
        return XmlReader.TYPE;
    }

    public void setupHandle(String xmlFileName) throws IOException {
        super.setupHandle();

        FileUtils.writeStringToFile(new File("working", TestUtils.XML_BASIC), TestUtils.getTestXMLFileContent(TestUtils.XML_BASIC));
        FileUtils.writeStringToFile(new File("working", TestUtils.XML_SINGLE_LINE), TestUtils.getTestXMLFileContent(TestUtils.XML_SINGLE_LINE));
        
        Resource resource = new Resource();
        resource.setType(LocalFile.TYPE);
        resource.put(LocalFile.LOCALFILE_PATH, "working");
        
        LocalFile localFile = new LocalFile();
        localFile.start(new ResourceFactory(), resource, null);

        when(component.getResource()).thenReturn(resource);        
        when(component.getResourceId()).thenReturn(resource.getId());
        Map<String, IResourceRuntime> deployedResources = new HashMap<>();
        deployedResources.put(component.getResourceId(), localFile);
        when(context.getDeployedResources()).thenReturn(deployedResources);        
        
        ((XmlReader) spy).runWhen = XmlReader.PER_MESSAGE;

    }
}
