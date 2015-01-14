package org.jumpmind.symmetric.is.core.runtime.component;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.flow.NodeRuntime;
import org.jumpmind.symmetric.is.core.utils.DbTestUtils;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DbReaderComponentTest {

	private static IDatabasePlatform platform;
	private static IComponentFactory componentFactory;
	private static IConnectionFactory connectionFactory;
	private static ExecutorService threadService;
	private static ComponentFlowNode readerComponentFlowNode;
	private static NodeRuntime readerNodeRuntime;
    
	@BeforeClass
	public static void setup() throws Exception {
	
    	componentFactory = new ComponentFactory();
    	connectionFactory = new ConnectionFactory();
    	threadService = Executors.newFixedThreadPool(5);
		platform = createPlatformAndTestDatabase();
		readerComponentFlowNode = createReaderComponentFlowNode();
	    readerNodeRuntime = new NodeRuntime(componentFactory.create(readerComponentFlowNode));
	}	
    
    @After
    public void tearDown() throws Exception {
    }
	
	@Test
	public void testReaderHeaderParameters() throws Exception {

	}
	
	@Test
	public void testReaderMessageParameters() throws Exception {
		
	}
	
	@Test
	public void testReaderHeaderAndMsgParameters() throws Exception {
		
	}
	
	@Test
	public void testReaderFlowFromStartupMsg() throws Exception {
		
		Message startupMsg = new StartupMessage();
		MessageTarget msgTarget = new MessageTarget();
		readerNodeRuntime.getComponent().handle(startupMsg, msgTarget);
		
		//Assert.assertEquals(1, flowRuntime.getComponentStatistics("Target Node 2").getNumberInboundMessages()); 
		

	}
   
	@Test
	public void testReaderFlowFromSingleContentMsg() throws Exception {
		   
	}
	   
	@Test
	public void testReaderFlowFromMultipleContentMsgs() throws Exception {
		   
	}

	
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

       Message targetMsg;
       
	   @Override
        public void put(Message message) {
        	targetMsg = message;
        }
    }
}
