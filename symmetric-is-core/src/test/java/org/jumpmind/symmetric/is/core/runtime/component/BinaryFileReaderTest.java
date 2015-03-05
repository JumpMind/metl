package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileOutputStream;
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
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.localfile.DASNASConnection;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class BinaryFileReaderTest {

    private static IConnectionFactory connectionFactory;
    private static ComponentFlowNode readerComponentFlowNode;
    private static final String FILE_PATH = "build/files/";
    private static final String FILE_NAME = "binary_test.bin";
    private static final String FILE_DATA = "This is a binary file to be read.";

    @BeforeClass
    public static void setup() throws Exception {

        connectionFactory = new ConnectionFactory();
        createTestFileToRead();
        readerComponentFlowNode = createReaderComponentFlowNode();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testBinarytReaderFlowFromStartupMsg() throws Exception {

        BinaryFileReaderComponent reader = new BinaryFileReaderComponent();
        reader.setComponentFlowNode(readerComponentFlowNode);
        reader.start(null, connectionFactory);
        Message msg = new StartupMessage();
        MessageTarget msgTarget = new MessageTarget();
        reader.handle(msg, msgTarget);

        assertEquals(1, msgTarget.getTargetMessageCount());
        assertArrayEquals((byte[]) msgTarget.getMessage(0).getPayload(),
                FILE_DATA.getBytes());
    }

    private static void createTestFileToRead() throws Exception {
        createTestDirectory();
        byte[] data = FILE_DATA.getBytes();
        FileOutputStream outStream = new FileOutputStream(FILE_PATH + FILE_NAME);
        outStream.write(data);
        outStream.close();
    }

    private static void createTestDirectory() {
        File dir = new File(FILE_PATH);
        if (!dir.exists()) {
            dir.mkdir();
        }
    }

    private static ComponentFlowNode createReaderComponentFlowNode() {

        Folder folder = TestUtils.createFolder("Test Folder");
        ComponentFlowVersion flow = TestUtils.createFlow("TestFlow", folder);
        Component component = TestUtils.createComponent(BinaryFileReaderComponent.TYPE, false);
        Setting[] settingData = createReaderSettings();
        ComponentVersion componentVersion = TestUtils.createComponentVersion(component, null,
                settingData);
        componentVersion.setConnection(createConnection(createConnectionSettings()));
        ComponentFlowNode readerComponent = new ComponentFlowNode();
        readerComponent.setComponentFlowVersionId(flow.getId());
        readerComponent.setComponentVersionId(componentVersion.getId());
        readerComponent.setCreateBy("Test");
        readerComponent.setCreateTime(new Date());
        readerComponent.setLastModifyBy("Test");
        readerComponent.setLastModifyTime(new Date());
        readerComponent.setComponentVersion(componentVersion);
        return readerComponent;
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

    private static Setting[] createReaderSettings() {
        Setting[] settings = new Setting[2];
        settings[0] = new Setting(BinaryFileReaderComponent.BINARYFILEREADER_RELATIVE_PATH, FILE_NAME);
        settings[1] = new Setting(BinaryFileReaderComponent.BINARYFILEREADER_SIZE_PER_MESSAGE, "1");
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
