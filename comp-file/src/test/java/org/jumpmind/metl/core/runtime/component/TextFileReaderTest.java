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

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.ExecutionTrackerNoOp;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.metl.core.utils.TestUtils;
import org.jumpmind.properties.TypedProperties;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TextFileReaderTest {

    private static IResourceRuntime resourceRuntime;
    private static Map<String, IResourceRuntime> deployedResources;
    private static FlowStep readerFlow;
    private static final String FILE_PATH = "build/files/";
    private static final String FILE_NAME = "text_test.txt";

    @BeforeClass
    public static void setup() throws Exception {
        createTestFileToRead();
        readerFlow = createTextReaderFlowStep();
        Resource resource = readerFlow.getComponent().getResource();
        deployedResources = new HashMap<>();
        resourceRuntime = new LocalFile();
        TypedProperties properties = new TypedProperties();
        properties.put(LocalFile.LOCALFILE_PATH, FILE_PATH);
        resourceRuntime.start(resource, properties);
        deployedResources.put(resource.getId(), resourceRuntime);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testTextReaderFlowFromStartupMsgSingleRowPerMessage() throws Exception {

        TextFileReader reader = new TextFileReader();
        reader.setContext(new ComponentContext(null, readerFlow, null, new ExecutionTrackerNoOp(), deployedResources, null, null,null));
        reader.start();
        Message msg = new ControlMessage();
        SendMessageCallback<ArrayList<String>> msgTarget = new SendMessageCallback<ArrayList<String>>();
        reader.handle(msg, msgTarget, true);

        assertEquals(4, msgTarget.getPayloadList().size());
        ArrayList<String> payload;
        payload = msgTarget.getPayloadList().get(0);
        assertEquals("This is the first line to read", payload.get(0));
        payload = msgTarget.getPayloadList().get(1);
        assertEquals("This is the second line to read", payload.get(0));
        payload = msgTarget.getPayloadList().get(2);
        assertEquals("This is the third line to read", payload.get(0));
        payload = msgTarget.getPayloadList().get(3);
        assertEquals("This is the fourth line to read", payload.get(0));

    }

    @Test
    public void testTextReaderFlowFromStartupMsgMultipleRowsPerMessage() throws Exception {

        TextFileReader reader = new TextFileReader();
        reader.setContext(new ComponentContext(null, readerFlow, null, new ExecutionTrackerNoOp(), deployedResources, null, null,null));
        reader.start();
        Message msg = new ControlMessage();
        SendMessageCallback<ArrayList<String>> msgTarget = new SendMessageCallback<ArrayList<String>>();
        reader.handle(msg, msgTarget, true);

        assertEquals(4, msgTarget.getPayloadList().size());
        ArrayList<String> payload;
        payload = msgTarget.getPayloadList().get(0);
        assertEquals("This is the first line to read", payload.get(0));
        payload = msgTarget.getPayloadList().get(1);
        assertEquals("This is the second line to read", payload.get(0));
        payload = msgTarget.getPayloadList().get(2);
        assertEquals("This is the third line to read", payload.get(0));
        payload = msgTarget.getPayloadList().get(3);
        assertEquals("This is the fourth line to read", payload.get(0));

    }

    private static void createTestFileToRead() throws Exception {
        createTestDirectory();
        PrintWriter writer = new PrintWriter(FILE_PATH + FILE_NAME);
        writer.println("This is a header row to skip");
        writer.println("This is the first line to read");
        writer.println("This is the second line to read");
        writer.println("This is the third line to read");
        writer.println("This is the fourth line to read");
        writer.close();
    }

    private static void createTestDirectory() {
        File dir = new File(FILE_PATH);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    private static FlowStep createTextReaderFlowStep() {

        Folder folder = TestUtils.createFolder("Test Folder");
        Flow flow = TestUtils.createFlow("TestFlow", folder);
        Setting[] settingData = createReaderSettings();
        Component component = TestUtils.createComponent(TextFileReader.TYPE, false, 
                createResource(createResourceSettings()), null,
                null, null, null, settingData);
        component.setResource(createResource(createResourceSettings()));
        FlowStep readerComponent = new FlowStep();
        readerComponent.setFlowId(flow.getId());
        readerComponent.setComponentId(component.getId());
        readerComponent.setCreateBy("Test");
        readerComponent.setCreateTime(new Date());
        readerComponent.setLastUpdateBy("Test");
        readerComponent.setLastUpdateTime(new Date());
        readerComponent.setComponent(component);
        return readerComponent;
    }

    private static Resource createResource(List<Setting> settings) {
        Resource resource = new Resource();
        Folder folder = TestUtils.createFolder("Test Folder Resource");
        resource.setName("Test Resource");
        resource.setFolderId("Test Folder Resource");
        resource.setType(LocalFile.TYPE);
        resource.setFolder(folder);
        resource.setSettings(settings);

        return resource;
    }

    private static Setting[] createReaderSettings() {

        Setting[] settingData = new Setting[3];
        settingData[0] = new Setting(TextFileReader.SETTING_RELATIVE_PATH, FILE_NAME);
        settingData[1] = new Setting(TextFileReader.SETTING_HEADER_LINES_TO_SKIP, "1");
        settingData[2] = new Setting(TextFileReader.SETTING_ROWS_PER_MESSAGE, "1");

        return settingData;
    }

    private static List<Setting> createResourceSettings() {
        List<Setting> settings = new ArrayList<Setting>(2);
        settings.add(new Setting(LocalFile.LOCALFILE_PATH, FILE_PATH));
        settings.add(new Setting(LocalFile.LOCALFILE_MUST_EXIST, "true"));
        return settings;
    }

}
