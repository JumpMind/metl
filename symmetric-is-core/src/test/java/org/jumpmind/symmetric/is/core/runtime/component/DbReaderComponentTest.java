package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import java.io.Serializable;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.config.Folder;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowNodeData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.utils.DbTestUtils;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;


@RunWith(PowerMockRunner.class)
public class DbReaderComponentTest {

	private static IConnectionFactory connectionFactory;
	private static ComponentFlowNode flowNode;
	private static IDatabasePlatform platform;
	private static ComponentFlowNode readerComponentFlowNode;

	//	private static IComponentFactory componentFactory;
//	private static ExecutorService threadService;
//	private static NodeRuntime readerNodeRuntime;
    
	@BeforeClass
	public static void setup() throws Exception {

		connectionFactory = new ConnectionFactory();
		platform = createPlatformAndTestDatabase();
		readerComponentFlowNode = createReaderComponentFlowNode();		

		//    	componentFactory = new ComponentFactory();
//    	threadService = Executors.newFixedThreadPool(5);

//	    readerNodeRuntime = new NodeRuntime(componentFactory.create(readerComponentFlowNode));
	}	
	
    @After
    public void tearDown() throws Exception {
    }
	  
	@Test
	public void testReaderParmsFromHeader() throws Exception {
		
		DbReaderComponent reader = new DbReaderComponent();
		Message inputMessage = new StartupMessage();
		Map<String, Serializable> msgParamMap = new HashMap<String, Serializable>();
		msgParamMap.put("param1", "abcde");
		msgParamMap.put("param2", new Integer(5));
		inputMessage.getHeader().setParameters(msgParamMap);
		Map<String, Object> inputParamMap = new HashMap<String, Object>();
		reader.setParamsFromInboundMsgAndRec(inputParamMap, inputMessage, null);
		
		assertEquals("abcde", inputParamMap.get("param1"));
		assertEquals(new Integer(5),inputParamMap.get("param2"));
	}
	
	@Test
	public void testReaderParmsFromMsgBody() throws Exception {

		DbReaderComponent reader = new DbReaderComponent();
		Message inputMessage = new StartupMessage();
		Map<String, Object> inputParamMap = new HashMap<String, Object>();
		EntityData inputRec = new EntityData();
		inputRec.put("param1", "fghij");
		inputRec.put("param2", new Integer(7));
		reader.setParamsFromInboundMsgAndRec(inputParamMap, inputMessage, inputRec);
		
		assertEquals("fghij", inputParamMap.get("param1"));
		assertEquals(new Integer(7),inputParamMap.get("param2"));
	}
	
	@Test 
	public void testReaderParmsFromHeaderAndMsgBody() throws Exception {
		
		DbReaderComponent reader = new DbReaderComponent();
		Message inputMessage = new StartupMessage();
		Map<String, Serializable> msgParamMap = new HashMap<String, Serializable>();
		msgParamMap.put("param1", "abcde");
		msgParamMap.put("param2", new Integer(5));
		inputMessage.getHeader().setParameters(msgParamMap);
		EntityData inputRec = new EntityData();
		inputRec.put("param3", "fghij");
		inputRec.put("param4", new Integer(7));		
		Map<String, Object> inputParamMap = new HashMap<String, Object>();
		reader.setParamsFromInboundMsgAndRec(inputParamMap, inputMessage, inputRec);
		
		assertEquals("abcde", inputParamMap.get("param1"));
		assertEquals(new Integer(5),inputParamMap.get("param2"));
		assertEquals("fghij", inputParamMap.get("param3"));
		assertEquals(new Integer(7),inputParamMap.get("param4"));
	}
	
	@Test
	public void testShutdownPassthrough() throws Exception {
		
		DbReaderComponent reader = spy(new DbReaderComponent());
		doReturn(null).when(reader).getJdbcTemplate();
		Message shutdownMessage = new ShutdownMessage();
		MessageTarget target = new MessageTarget();
		reader.handle(shutdownMessage, target);
		
		assertEquals(1,target.getTargetMessageCount());
		assertEquals(true,target.getMessage(0) instanceof ShutdownMessage);		
	}
	
//	@Test
//	public void testReaderFlowFromStartupMsg() throws Exception {
//
//	}
//   
//	@Test
//	public void testReaderFlowFromSingleContentMsg() throws Exception {
//		   
//	}
//	   
//	@Test
//	public void testReaderFlowFromMultipleContentMsgs() throws Exception {
//		   
//	}

	
	private static ComponentFlowNode createReaderComponentFlowNode() {
		
		final String name="Test Reader 1";		
	 	Folder folder = TestUtils.createFolder("Test Folder");
    	ComponentFlowVersion flow = TestUtils.createFlow("TestFlow", folder);	 	
    	Component component = TestUtils.createComponent(DbReaderComponent.TYPE, false);
    	SettingData[] settingData = createReaderSettings();
    	ComponentVersion componentVersion = TestUtils.createComponentVersion(component, name, null, settingData);
    	ComponentFlowNodeData data = new ComponentFlowNodeData();
    	data.setComponentFlowVersionId(flow.getId());
    	data.setComponentVersionId(componentVersion.getId());
    	data.setCreateBy("Test");
    	data.setCreateTime(new Date());
    	data.setId(name);
    	data.setLastModifyBy("Test");
    	data.setLastModifyTime(new Date());
    	ComponentFlowNode readerComponent = new ComponentFlowNode(componentVersion, data);
        	
	 	return readerComponent;
	}
	
	private static SettingData[] createReaderSettings() {
		
		SettingData[] settingData = new SettingData[2];
		settingData[0] = new SettingData(DbReaderComponent.SQL,"select * From test_table_1");
		settingData[1] = new SettingData(DbReaderComponent.ROWS_PER_MESSAGE,"2");
		
		return settingData;
	}
	
	private static IDatabasePlatform createPlatformAndTestDatabase() throws Exception {
		
		platform = DbTestUtils.createDatabasePlatform();
		Database database = createTestDatabase();
		platform.createDatabase(database, true, false);
		populateTestDatabase(platform, database);
		
		return platform;
	}
	
	private static Database createTestDatabase() {
		
		Table testTable1 = createTestTable1();
		Database database = new Database();
		database.addTable(testTable1);
		return database;
	}
	
	private static Table createTestTable1() {
		
		Table table = new Table("test_table_1");

		List<Column> columns = new ArrayList<Column>();
		columns.add(new Column("col1",true,Types.INTEGER,4,1));
		columns.add(new Column("col2",false,Types.VARCHAR,50,50));
		columns.add(new Column("col3",false,Types.DECIMAL,9,2));
		
		table.addColumns(columns);
		return table;
	}
	
	private static void populateTestDatabase(IDatabasePlatform platform, Database database) {
		
		ISqlTemplate template = platform.getSqlTemplate();
		DmlStatement statement = platform.createDmlStatement(DmlType.INSERT, database.findTable("test_table_1"), null);
		template.update(statement.getSql(), statement.getValueArray(new Object[] {1,"test row 1",7.7}, new Object[] {1}));
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
