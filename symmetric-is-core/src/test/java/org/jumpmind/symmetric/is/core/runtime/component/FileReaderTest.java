package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.Setting;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.localfile.DASNASConnection;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class FileReaderTest {

	private static IConnectionFactory connectionFactory;
	private static ComponentFlowNode readerComponentFlowNode;    
    private static final String FILE_PATH = "build/files/";
    private static final String TEXT_FILE_NAME = "text_test.txt";
    private static final String BINARY_FILE_NAME="binary_test.bin";
    private static final String BINARY_FILE_DATA="This is a binary file to be read.";
    
	@BeforeClass
	public static void setup() throws Exception {

		connectionFactory = new ConnectionFactory();
		createTestTextFileToRead();
		createTestBinaryFileToRead();
		readerComponentFlowNode = createReaderComponentFlowNode();		
	}	
	
	
	@Test
	public void testTextReaderFlowFromStartupMsgSingleRowPerMessage() throws Exception {

		FileReaderComponent reader = new FileReaderComponent();
		reader.setComponentFlowNode(readerComponentFlowNode);
		reader.start(null, connectionFactory);
		Message msg = new StartupMessage();
		MessageTarget msgTarget = new MessageTarget();
		reader.handle(msg, msgTarget);

		assertEquals(4,msgTarget.getTargetMessageCount());
		ArrayList<String> payload;
		payload = msgTarget.getMessage(0).getPayload();
		assertEquals("This is the first line to read",payload.get(0));
		payload = msgTarget.getMessage(1).getPayload();
		assertEquals("This is the second line to read",payload.get(0));
		payload = msgTarget.getMessage(2).getPayload();
		assertEquals("This is the third line to read",payload.get(0));
		payload = msgTarget.getMessage(3).getPayload();
		assertEquals("This is the fourth line to read",payload.get(0));

	}

	@Test
	public void testTextReaderFlowFromStartupMsgMultipleRowsPerMessage() throws Exception {

		FileReaderComponent reader = new FileReaderComponent();
		reader.setComponentFlowNode(readerComponentFlowNode);
		reader.start(null, connectionFactory);
		Message msg = new StartupMessage();
		MessageTarget msgTarget = new MessageTarget();
		reader.handle(msg, msgTarget);

		assertEquals(4,msgTarget.getTargetMessageCount());
		ArrayList<String> payload;
		payload = msgTarget.getMessage(0).getPayload();
		assertEquals("This is the first line to read",payload.get(0));
		payload = msgTarget.getMessage(1).getPayload();
		assertEquals("This is the second line to read",payload.get(0));
		payload = msgTarget.getMessage(2).getPayload();
		assertEquals("This is the third line to read",payload.get(0));
		payload = msgTarget.getMessage(3).getPayload();
		assertEquals("This is the fourth line to read",payload.get(0));

	}
	
	@Test
	public void testBinarytReaderFlowFromStartupMsg() throws Exception {

		FileReaderComponent reader = new FileReaderComponent();
		readerComponentFlowNode.getComponentVersion().getSettings().clear();
		readerComponentFlowNode.getComponentVersion().getSettings().
		addAll(createBinaryReaderSettings());
		reader.setComponentFlowNode(readerComponentFlowNode);
		reader.start(null, connectionFactory);
		Message msg = new StartupMessage();
		MessageTarget msgTarget = new MessageTarget();
		reader.handle(msg, msgTarget);

		assertEquals(1,msgTarget.getTargetMessageCount());
		assertArrayEquals((byte[]) msgTarget.getMessage(0).getPayload(),BINARY_FILE_DATA.getBytes());
	}
	
	private static void createTestTextFileToRead() throws Exception {
		createTestDirectory();
		PrintWriter writer = new PrintWriter(FILE_PATH + TEXT_FILE_NAME);
		writer.println("This is a header row to skip");
        writer.println("This is the first line to read");
        writer.println("This is the second line to read");
        writer.println("This is the third line to read");
        writer.println("This is the fourth line to read");
        writer.close();
	}
	
	private static void createTestBinaryFileToRead() throws Exception {
		createTestDirectory();
		byte[] data = BINARY_FILE_DATA.getBytes();
		FileOutputStream outStream = new FileOutputStream(FILE_PATH + BINARY_FILE_NAME);
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
    	Component component = TestUtils.createComponent(FileReaderComponent.TYPE, false);
    	Setting[] settingData = createReaderSettings();
    	ComponentVersion componentVersion = TestUtils.createComponentVersion(component, null, settingData);
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
		
		Setting[] settingData = new Setting[4];
		settingData[0] = new Setting(FileReaderComponent.FILEREADER_RELATIVE_PATH,TEXT_FILE_NAME);
		settingData[1] = new Setting(FileReaderComponent.FILEREADER_FILE_TYPE,FileReaderComponent.FILE_TYPE_TEXT);
		settingData[2] = new Setting(FileReaderComponent.FILEREADER_TEXT_HEADER_LINES_TO_SKIP,"1");
		settingData[3] = new Setting(FileReaderComponent.FILEREADER_TEXT_ROWS_PER_MESSAGE,"1");
		
		return settingData;
	}
	
	private static ArrayList<Setting> createBinaryReaderSettings() {
		ArrayList<Setting> settings = new ArrayList<Setting>(3);
		settings.add(new Setting(FileReaderComponent.FILEREADER_RELATIVE_PATH,BINARY_FILE_NAME));
		settings.add(new Setting(FileReaderComponent.FILEREADER_FILE_TYPE,FileReaderComponent.FILE_TYPE_BINARY));
		settings.add(new Setting(FileReaderComponent.FILEREADER_BINARY_SIZE_PER_MESSAGE,"1"));		
		return settings;
	}
	
	private static List<Setting> createConnectionSettings() {
		List<Setting> settings = new ArrayList<Setting>(2);
		settings.add(new Setting(DASNASConnection.DASNAS_PATH,FILE_PATH));
		settings.add(new Setting(DASNASConnection.DASNAS_MUST_EXIST,"true"));
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
