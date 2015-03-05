package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.PrintWriter;
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
public class TextFileReaderTest {

    private static IConnectionFactory connectionFactory;
    private static ComponentFlowNode readerComponentFlowNode;
    private static final String FILE_PATH = "build/files/";
    private static final String FILE_NAME = "text_test.txt";

    @BeforeClass
    public static void setup() throws Exception {

        connectionFactory = new ConnectionFactory();
        createTestFileToRead();
        readerComponentFlowNode = createTextReaderComponentFlowNode();
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testTextReaderFlowFromStartupMsgSingleRowPerMessage() throws Exception {

        TextFileReaderComponent reader = new TextFileReaderComponent();
        reader.setComponentFlowNode(readerComponentFlowNode);
        reader.start(null, connectionFactory);
        Message msg = new StartupMessage();
        MessageTarget msgTarget = new MessageTarget();
        reader.handle(msg, msgTarget);

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

        TextFileReaderComponent reader = new TextFileReaderComponent();
        reader.setComponentFlowNode(readerComponentFlowNode);
        reader.start(null, connectionFactory);
        Message msg = new StartupMessage();
        MessageTarget msgTarget = new MessageTarget();
        reader.handle(msg, msgTarget);

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

    private static ComponentFlowNode createTextReaderComponentFlowNode() {

        Folder folder = TestUtils.createFolder("Test Folder");
        ComponentFlowVersion flow = TestUtils.createFlow("TestFlow", folder);
        Component component = TestUtils.createComponent(TextFileReaderComponent.TYPE, false);
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

        Setting[] settingData = new Setting[3];
        settingData[0] = new Setting(TextFileReaderComponent.TEXTFILEREADER_RELATIVE_PATH, FILE_NAME);
        settingData[1] = new Setting(TextFileReaderComponent.TEXTFILEREADER_HEADER_LINES_TO_SKIP, "1");
        settingData[2] = new Setting(TextFileReaderComponent.TEXTFILEREADER_ROWS_PER_MESSAGE, "1");

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
