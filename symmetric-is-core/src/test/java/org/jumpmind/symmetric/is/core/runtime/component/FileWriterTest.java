package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;
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

public class FileWriterTest {

    private static IConnectionFactory connectionFactory;
    private static ComponentFlowNode writerComponentFlowNode;
    private static final String FILE_PATH = "build/files/";
    private static final String TEXT_FILE_NAME = "text_test_writer.txt";
    private static final String BINARY_FILE_NAME = "binary_test_writer.bin";
    private static final String BINARY_FILE_DATA = "This is a binary file to be written.";

    @BeforeClass
    public static void setup() throws Exception {
        connectionFactory = new ConnectionFactory();
        writerComponentFlowNode = createWriterComponentFlowNode();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testTextWriterMultipleRowsPerMessage() throws Exception {
        FileWriterComponent writer = new FileWriterComponent();
        writer.setComponentFlowNode(writerComponentFlowNode);
        writer.start(null, connectionFactory);
        writer.handle(createMultipleRowTextMessageToWrite(), null);
        checkTextFile();
    }

    @Test
    public void testTextWriterSingleRowPerMessage() throws Exception {
        FileWriterComponent writer = new FileWriterComponent();
        writer.setComponentFlowNode(writerComponentFlowNode);
        writer.start(null, connectionFactory);
        writer.handle(createSingleRowTextMessageToWrite(1), null);
        writer.handle(createSingleRowTextMessageToWrite(2), null);
        writer.handle(createSingleRowTextMessageToWrite(3), null);
        writer.handle(createSingleRowTextMessageToWrite(4), null);
        checkTextFile();
    }

    @Test
    public void testBinaryWriter() throws Exception {
        FileWriterComponent writer = new FileWriterComponent();
        writerComponentFlowNode.getComponentVersion().getSettings().clear();
        writerComponentFlowNode.getComponentVersion().getSettings()
                .addAll(createBinaryWriterSettings());
        writer.setComponentFlowNode(writerComponentFlowNode);
        writer.start(null, connectionFactory);
        writer.handle(createBinaryMessageToWrite(), null);
        checkBinaryFile();
    }

    private static void checkTextFile() throws Exception {
        Path path = Paths.get(FILE_PATH + TEXT_FILE_NAME);
        List<String> fileLines = Files.readAllLines(path, StandardCharsets.UTF_8);
        assertEquals(4, fileLines.size());
        assertEquals("Line 1", fileLines.get(0));
        assertEquals("Line 2", fileLines.get(1));
        assertEquals("Line 3", fileLines.get(2));
        assertEquals("Line 4", fileLines.get(3));
    }

    private static void checkBinaryFile() throws Exception {
        Path path = Paths.get(FILE_PATH + BINARY_FILE_NAME);
        byte[] fileData = Files.readAllBytes(path);
        assertArrayEquals(fileData, BINARY_FILE_DATA.getBytes());
    }

    private static Message createMultipleRowTextMessageToWrite() {
        Message msg = new Message("originating node id");
        ArrayList<String> payload = new ArrayList<String>();
        payload.add("Line 1");
        payload.add("Line 2");
        payload.add("Line 3");
        payload.add("Line 4");
        msg.setPayload(payload);
        return msg;
    }

    private static Message createBinaryMessageToWrite() {
        Message msg = new Message("originating node id");
        byte[] payload = BINARY_FILE_DATA.getBytes();
        msg.setPayload(payload);
        return msg;
    }

    private static Message createSingleRowTextMessageToWrite(int lineNumber) {
        Message msg = new Message("originating node id");
        ArrayList<String> payload = new ArrayList<String>();
        payload.add("Line " + lineNumber);
        msg.setPayload(payload);
        return msg;
    }

    private static ComponentFlowNode createWriterComponentFlowNode() {
        Folder folder = TestUtils.createFolder("Test Folder");
        ComponentFlowVersion flow = TestUtils.createFlow("TestFlow", folder);
        Component component = TestUtils.createComponent(FileWriterComponent.TYPE, false);
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
        Setting[] settingData = new Setting[2];
        settingData[0] = new Setting(FileWriterComponent.FILEWRITER_RELATIVE_PATH, TEXT_FILE_NAME);
        settingData[1] = new Setting(FileWriterComponent.FILEWRITER_FILE_TYPE,
                FileType.TEXT);

        return settingData;
    }

    public static ArrayList<Setting> createBinaryWriterSettings() {
        ArrayList<Setting> settings = new ArrayList<Setting>(2);
        settings.add(new Setting(FileWriterComponent.FILEWRITER_RELATIVE_PATH, BINARY_FILE_NAME));
        settings.add(new Setting(FileWriterComponent.FILEWRITER_FILE_TYPE,
                FileType.BINARY));
        return settings;
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
