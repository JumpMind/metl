package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertArrayEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.localfile.LocalFileResource;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class BinaryFileWriterTest {

    private static IResourceFactory resourceFactory;
    private static FlowStep writerFlowStep;
    private static final String FILE_PATH = "build/files/";
    private static final String FILE_NAME = "binary_test_writer.bin";
    private static final String FILE_DATA = "This is a binary file to be written.";

    @BeforeClass
    public static void setup() throws Exception {
        resourceFactory = new ResourceFactory();
        writerFlowStep = createWriterFlowStep();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBinaryWriter() throws Exception {
        BinaryFileWriter writer = new BinaryFileWriter();
        writer.setFlowStep(writerFlowStep);
        writer.start(null, resourceFactory);
        writer.handle(createBinaryMessageToWrite(), null);
        checkBinaryFile();
    }

    private static void checkBinaryFile() throws Exception {
        Path path = Paths.get(FILE_PATH + FILE_NAME);
        byte[] fileData = Files.readAllBytes(path);
        assertArrayEquals(fileData, FILE_DATA.getBytes());
    }

    private static Message createBinaryMessageToWrite() {
        Message msg = new Message("originating step id");
        byte[] payload = FILE_DATA.getBytes();
        msg.setPayload(payload);
        msg.getHeader().setSequenceNumber(1);
        msg.getHeader().setLastMessage(true);
        return msg;
    }

    private static FlowStep createWriterFlowStep() {
        Folder folder = TestUtils.createFolder("Test Folder");
        FlowVersion flow = TestUtils.createFlowVersion("TestFlow", folder);
        Component component = TestUtils.createComponent(BinaryFileWriter.TYPE, false);
        Setting[] settingData = createWriterSettings();
        ComponentVersion componentVersion = TestUtils.createComponentVersion(component, null,
                settingData);
        componentVersion.setResource(createResource(createResourceSettings()));
        FlowStep writerComponent = new FlowStep();
        writerComponent.setFlowVersionId(flow.getId());
        writerComponent.setComponentVersionId(componentVersion.getId());
        writerComponent.setCreateBy("Test");
        writerComponent.setCreateTime(new Date());
        writerComponent.setLastModifyBy("Test");
        writerComponent.setLastModifyTime(new Date());
        writerComponent.setComponentVersion(componentVersion);
        return writerComponent;
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

    private static Setting[] createWriterSettings() {
        Setting[] settingData = new Setting[1];
        settingData[0] = new Setting(BinaryFileWriter.BINARYFILEWRITER_RELATIVE_PATH, FILE_NAME);
        return settingData;
    }

    private static List<Setting> createResourceSettings() {
        List<Setting> settings = new ArrayList<Setting>(2);
        settings.add(new Setting(LocalFileResource.DASNAS_PATH, FILE_PATH));
        settings.add(new Setting(LocalFileResource.DASNAS_MUST_EXIST, "true"));
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
