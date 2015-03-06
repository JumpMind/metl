package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertArrayEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.model.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.model.Connection;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.localfile.DASNASConnection;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class BinaryFileWriterTest {

    private static IConnectionFactory connectionFactory;
    private static ComponentFlowNode writerComponentFlowNode;
    private static final String FILE_PATH = "build/files/";
    private static final String FILE_NAME = "binary_test_writer.bin";
    private static final String FILE_DATA = "This is a binary file to be written.";

    @BeforeClass
    public static void setup() throws Exception {
        connectionFactory = new ConnectionFactory();
        writerComponentFlowNode = createWriterComponentFlowNode();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBinaryWriter() throws Exception {
        BinaryFileWriter writer = new BinaryFileWriter();
        writer.setComponentFlowNode(writerComponentFlowNode);
        writer.start(null, connectionFactory);
        writer.handle(createBinaryMessageToWrite(), null);
        checkBinaryFile();
    }

    private static void checkBinaryFile() throws Exception {
        Path path = Paths.get(FILE_PATH + FILE_NAME);
        byte[] fileData = Files.readAllBytes(path);
        assertArrayEquals(fileData, FILE_DATA.getBytes());
    }

    private static Message createBinaryMessageToWrite() {
        Message msg = new Message("originating node id");
        byte[] payload = FILE_DATA.getBytes();
        msg.setPayload(payload);
        msg.getHeader().setSequenceNumber(1);
        msg.getHeader().setLastMessage(true);
        return msg;
    }

    private static ComponentFlowNode createWriterComponentFlowNode() {
        Folder folder = TestUtils.createFolder("Test Folder");
        ComponentFlowVersion flow = TestUtils.createFlow("TestFlow", folder);
        Component component = TestUtils.createComponent(BinaryFileWriter.TYPE, false);
        Setting[] settingData = createWriterSettings();
        ComponentVersion componentVersion = TestUtils.createComponentVersion(component, null,
                settingData);
        componentVersion.setConnection(createConnection(createConnectionSettings()));
        ComponentFlowNode writerComponent = new ComponentFlowNode();
        writerComponent.setComponentFlowVersionId(flow.getId());
        writerComponent.setComponentVersionId(componentVersion.getId());
        writerComponent.setCreateBy("Test");
        writerComponent.setCreateTime(new Date());
        writerComponent.setLastModifyBy("Test");
        writerComponent.setLastModifyTime(new Date());
        writerComponent.setComponentVersion(componentVersion);
        return writerComponent;
    }

    private static Connection createConnection(List<Setting> settings) {
        Connection connection = new Connection();
        Folder folder = TestUtils.createFolder("Test Folder Connection");
        connection.setName("Test Connection");
        connection.setFolderId("Test Folder Connection");
        connection.setType(DASNASConnection.TYPE);
        connection.setFolder(folder);
        connection.setSettings(settings);

        return connection;
    }

    private static Setting[] createWriterSettings() {
        Setting[] settingData = new Setting[1];
        settingData[0] = new Setting(BinaryFileWriter.BINARYFILEWRITER_RELATIVE_PATH, FILE_NAME);
        return settingData;
    }

    private static List<Setting> createConnectionSettings() {
        List<Setting> settings = new ArrayList<Setting>(2);
        settings.add(new Setting(DASNASConnection.DASNAS_PATH, FILE_PATH));
        settings.add(new Setting(DASNASConnection.DASNAS_MUST_EXIST, "true"));
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
