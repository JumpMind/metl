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

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
import org.jumpmind.metl.core.runtime.ExecutionTrackerNoOp;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.TextMessage;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.LocalFile;
import org.jumpmind.metl.core.utils.TestUtils;
import org.jumpmind.properties.TypedProperties;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class TextFileWriterTest {

    private static IResourceRuntime resourceRuntime;
    private static Map<String, IResourceRuntime> deployedResources;
    private static FlowStep writerFlowStep;
    private static final String FILE_PATH = "build/files/";
    private static final String FILE_NAME = "text_test_writer.txt";

    @BeforeClass
    public static void setup() throws Exception {
        writerFlowStep = createWriterFlowStep();
        Resource resource = writerFlowStep.getComponent().getResource();
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
    public void testTextWriterMultipleRowsPerMessage() throws Exception {
        TextFileWriter writer = new TextFileWriter();
        writer.setContext(new ComponentContext(null, writerFlowStep, null, new ExecutionTrackerNoOp(), deployedResources, null, null,null));
        writer.start();
        writer.handle(createMultipleRowTextMessageToWrite(), null, true);
        checkTextFile();
    }

    @Test
    public void testTextWriterSingleRowPerMessage() throws Exception {
        TextFileWriter writer = new TextFileWriter();
        writer.setContext(new ComponentContext(null, writerFlowStep, null, new ExecutionTrackerNoOp(), deployedResources, null, null,null));
        writer.start();
        writer.handle(createSingleRowTextMessageToWrite(1, false), null, true);
        writer.handle(createSingleRowTextMessageToWrite(2, false), null, true);
        writer.handle(createSingleRowTextMessageToWrite(3, false), null, true);
        writer.handle(createSingleRowTextMessageToWrite(4, true), null, true);
        checkTextFile();
    }

    private static void checkTextFile() throws Exception {
        Path path = Paths.get(FILE_PATH + FILE_NAME);
        List<String> fileLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertEquals(4, fileLines.size());
        assertEquals("Line 1", fileLines.get(0));
        assertEquals("Line 2", fileLines.get(1));
        assertEquals("Line 3", fileLines.get(2));
        assertEquals("Line 4", fileLines.get(3));
    }

    private static Message createMultipleRowTextMessageToWrite() {
        TextMessage msg = new TextMessage("originating step id");
        msg.getHeader().setSequenceNumber(1);
        ArrayList<String> payload = new ArrayList<String>();
        payload.add("Line 1");
        payload.add("Line 2");
        payload.add("Line 3");
        payload.add("Line 4");
        msg.setPayload(payload);
        return msg;
    }

    private static Message createSingleRowTextMessageToWrite(int lineNumber, boolean lastMsg) {
        TextMessage msg = new TextMessage("originating step id");
        msg.getHeader().setSequenceNumber(lineNumber);
        ArrayList<String> payload = new ArrayList<String>();
        payload.add("Line " + lineNumber);
        msg.setPayload(payload);
        return msg;
    }

    private static FlowStep createWriterFlowStep() {
        Folder folder = TestUtils.createFolder("Test Folder");
        Flow flow = TestUtils.createFlow("TestFlow", folder);
        Setting[] settingData = createWriterSettings();
        Component component = TestUtils.createComponent(TextFileWriter.TYPE, false, 
                createResource(createResourceSettings()), null, null, null, null, settingData);
        FlowStep writerComponent = new FlowStep();
        writerComponent.setFlowId(flow.getId());
        writerComponent.setComponentId(component.getId());
        writerComponent.setCreateBy("Test");
        writerComponent.setCreateTime(new Date());
        writerComponent.setLastUpdateBy("Test");
        writerComponent.setLastUpdateTime(new Date());
        writerComponent.setComponent(component);
        return writerComponent;
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

    private static Setting[] createWriterSettings() {
        Setting[] settingData = new Setting[1];
        settingData[0] = new Setting(TextFileWriter.SETTING_RELATIVE_PATH, FILE_NAME);

        return settingData;
    }

    private static List<Setting> createResourceSettings() {
        List<Setting> settings = new ArrayList<Setting>(2);
        settings.add(new Setting(LocalFile.LOCALFILE_PATH, FILE_PATH));
        settings.add(new Setting(LocalFile.LOCALFILE_MUST_EXIST, "true"));
        return settings;
    }
}
