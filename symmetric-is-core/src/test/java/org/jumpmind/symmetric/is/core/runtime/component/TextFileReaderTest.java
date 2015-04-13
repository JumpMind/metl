package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.LocalFileResource;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceFactory;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TextFileReaderTest {

    private static IResourceFactory resourceFactory;
    private static FlowStep readerFlow;
    private static final String FILE_PATH = "build/files/";
    private static final String FILE_NAME = "text_test.txt";

    @BeforeClass
    public static void setup() throws Exception {

        resourceFactory = new ResourceFactory();
        createTestFileToRead();
        readerFlow = createTextReaderFlowStep();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testTextReaderFlowFromStartupMsgSingleRowPerMessage() throws Exception {

        TextFileReader reader = new TextFileReader();
        reader.init(readerFlow, null);
        reader.start(null, resourceFactory);
        Message msg = new StartupMessage();
        MessageTarget msgTarget = new MessageTarget();
        reader.handle("test", msg, msgTarget);

        assertEquals(4, msgTarget.getTargetMessageCount());
        ArrayList<String> payload;
        payload = msgTarget.getMessage(0).getPayload();
        assertEquals("This is the first line to read", payload.get(0));
        payload = msgTarget.getMessage(1).getPayload();
        assertEquals("This is the second line to read", payload.get(0));
        payload = msgTarget.getMessage(2).getPayload();
        assertEquals("This is the third line to read", payload.get(0));
        payload = msgTarget.getMessage(3).getPayload();
        assertEquals("This is the fourth line to read", payload.get(0));

    }

    @Test
    public void testTextReaderFlowFromStartupMsgMultipleRowsPerMessage() throws Exception {

        TextFileReader reader = new TextFileReader();
        reader.init(readerFlow, null);
        reader.start(null, resourceFactory);
        Message msg = new StartupMessage();
        MessageTarget msgTarget = new MessageTarget();
        reader.handle("test", msg, msgTarget);

        assertEquals(4, msgTarget.getTargetMessageCount());
        ArrayList<String> payload;
        payload = msgTarget.getMessage(0).getPayload();
        assertEquals("This is the first line to read", payload.get(0));
        payload = msgTarget.getMessage(1).getPayload();
        assertEquals("This is the second line to read", payload.get(0));
        payload = msgTarget.getMessage(2).getPayload();
        assertEquals("This is the third line to read", payload.get(0));
        payload = msgTarget.getMessage(3).getPayload();
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
        readerComponent.setLastModifyBy("Test");
        readerComponent.setLastModifyTime(new Date());
        readerComponent.setComponent(component);
        return readerComponent;
    }

    private static Resource createResource(List<Setting> settings) {
        Resource resource = new Resource();
        Folder folder = TestUtils.createFolder("Test Folder Resource");
        resource.setName("Test Resource");
        resource.setFolderId("Test Folder Resource");
        resource.setType(LocalFileResource.TYPE);
        resource.setFolder(folder);
        resource.setSettings(settings);

        return resource;
    }

    private static Setting[] createReaderSettings() {

        Setting[] settingData = new Setting[3];
        settingData[0] = new Setting(TextFileReader.TEXTFILEREADER_RELATIVE_PATH, FILE_NAME);
        settingData[1] = new Setting(TextFileReader.TEXTFILEREADER_HEADER_LINES_TO_SKIP, "1");
        settingData[2] = new Setting(TextFileReader.TEXTFILEREADER_ROWS_PER_MESSAGE, "1");

        return settingData;
    }

    private static List<Setting> createResourceSettings() {
        List<Setting> settings = new ArrayList<Setting>(2);
        settings.add(new Setting(LocalFileResource.LOCALFILE_PATH, FILE_PATH));
        settings.add(new Setting(LocalFileResource.LOCALFILE_MUST_EXIST, "true"));
        return settings;
    }

    class MessageTarget implements IMessageTarget {

        List<Message> targetMsgArray = new ArrayList<Message>();

        @Override
        public void put(Message message) {
            targetMsgArray.add(message);
        }

        public Message getMessage(int idx) {
            return targetMsgArray.get(idx);
        }

        public int getTargetMessageCount() {
            return targetMsgArray.size();
        }
    }
}
