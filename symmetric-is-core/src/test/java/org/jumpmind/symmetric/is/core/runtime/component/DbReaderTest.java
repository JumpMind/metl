package org.jumpmind.symmetric.is.core.runtime.component;

import static org.junit.Assert.assertEquals;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.Folder;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.ExecutionTrackerNoOp;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.Datasource;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceRuntime;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceFactory;
import org.jumpmind.symmetric.is.core.utils.DbTestUtils;
import org.jumpmind.symmetric.is.core.utils.TestUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class DbReaderTest {

    private static IResourceRuntime resourceRuntime;
    private static IDatabasePlatform platform;
    private static FlowStep readerFlowStep;

    @BeforeClass
    public static void setup() throws Exception {
        platform = createPlatformAndTestDatabase();
        readerFlowStep = createReaderFlowStep();
        Resource resource = readerFlowStep.getComponent().getResource();
        resourceRuntime = new ResourceFactory().create(resource, null);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testReaderFlowFromStartupMsg() throws Exception {
        DbReader reader = new DbReader();
        reader.init(new ComponentContext(readerFlowStep, null, new ExecutionTrackerNoOp(), resourceRuntime, null));
        reader.start();
        Message msg = new StartupMessage();
        MessageTarget msgTarget = new MessageTarget();
        reader.handle( msg, msgTarget);

        assertEquals(2, msgTarget.getTargetMessageCount());
        ArrayList<EntityData> payload = msgTarget.getMessage(0).getPayload();
        assertEquals("test row 1", payload.get(0).get("tt1col2"));
        assertEquals("test row x", payload.get(0).get("tt2coly"));
    }

    @Test
    public void testReaderFlowFromSingleContentMsg() throws Exception {

        DbReader reader = new DbReader();
        reader.init(new ComponentContext(readerFlowStep, null, new ExecutionTrackerNoOp(), resourceRuntime, null));
        reader.start();
        Message message = new Message("fake step id");
        ArrayList<EntityData> inboundPayload = new ArrayList<EntityData>();
        inboundPayload.add(new EntityData());
        message.setPayload(inboundPayload);
        
        MessageTarget msgTarget = new MessageTarget();
        reader.handle(message, msgTarget);

        assertEquals(2, msgTarget.getTargetMessageCount());
        ArrayList<EntityData> payload = msgTarget.getMessage(0).getPayload();
        assertEquals("test row 1", payload.get(0).get("tt1col2"));
        assertEquals("test row x", payload.get(0).get("tt2coly"));
    }

    @Test
    public void testReaderFlowFromMultipleContentMsgs() throws Exception {

    }
    
    @Test
    public void testCountColumnSeparatingCommas() {
        
        DbReader reader = new DbReader();
        
        int count = reader.countColumnSeparatingCommas("ISNULL(a,''), b, *");
        assertEquals(count, 2);        
        count = reader.countColumnSeparatingCommas("ISNULL(a,('')), b, *");
        assertEquals(count,2);
    }

    @Test
    public void testGetSqlColumnEntityHints() throws Exception {
        
        DbReader reader = new DbReader();
        String sql = "select\r\n ISNULL(a,ISNULL(z,'')) /*COLA*/, b/*COLB*/, c/*  COLC */ from test;";
        Map<Integer, String> hints = reader.getSqlColumnEntityHints(sql);
        assertEquals(hints.get(1), "COLA");
        assertEquals(hints.get(2), "COLB");
        assertEquals(hints.get(3), "COLC");
        
    }
    
    private static FlowStep createReaderFlowStep() {

        Folder folder = TestUtils.createFolder("Test Folder");
        Flow flow = TestUtils.createFlow("TestFlow", folder);
        Setting[] settingData = createReaderSettings();
        Component componentVersion = TestUtils.createComponent(DbReader.TYPE, false,
                createResource(createResourceSettings()), null, createOutputModel(), null,
                null, settingData);
        FlowStep readerComponent = new FlowStep();
        readerComponent.setFlowId(flow.getId());
        readerComponent.setComponentId(componentVersion.getId());
        readerComponent.setCreateBy("Test");
        readerComponent.setCreateTime(new Date());
        readerComponent.setLastUpdateBy("Test");
        readerComponent.setLastUpdateTime(new Date());
        readerComponent.setComponent(componentVersion);
        return readerComponent;
    }

    private static Model createOutputModel() {

        ModelEntity tt1 = new ModelEntity("tt1", "TEST_TABLE_1");
        tt1.addModelAttribute(new ModelAttribute("tt1col1", tt1.getId(), "COL1"));
        tt1.addModelAttribute(new ModelAttribute("tt1col2", tt1.getId(), "COL2"));
        tt1.addModelAttribute(new ModelAttribute("tt1col3", tt1.getId(), "COL3"));

        ModelEntity tt2 = new ModelEntity("tt2", "TEST_TABLE_2");
        tt2.addModelAttribute(new ModelAttribute("tt2colx", tt1.getId(), "COLX"));
        tt2.addModelAttribute(new ModelAttribute("tt2coly", tt1.getId(), "COLY"));
        tt2.addModelAttribute(new ModelAttribute("tt2colz", tt1.getId(), "COLZ"));

        Model modelVersion = new Model();
        modelVersion.getModelEntities().add(tt1);
        modelVersion.getModelEntities().add(tt2);

        return modelVersion;
    }
    
    private static Resource createResource(List<Setting> settings) {
        Resource resource = new Resource();
        Folder folder = TestUtils.createFolder("Test Folder Resource");
        resource.setName("Test Resource");
        resource.setFolderId("Test Folder Resource");
        resource.setType(Datasource.TYPE);
        resource.setFolder(folder);
        resource.setSettings(settings);

        return resource;
    }

    private static Setting[] createReaderSettings() {

        Setting[] settingData = new Setting[2];
        settingData[0] = new Setting(DbReader.SQL,
                "select * From test_table_1 tt1 inner join test_table_2 tt2"
                + " on tt1.col1 = tt2.colx order by tt1.col1");
        settingData[1] = new Setting(DbReader.ROWS_PER_MESSAGE, "2");

        return settingData;
    }

    private static List<Setting> createResourceSettings() {
        List<Setting> settings = new ArrayList<Setting>(4);
        settings.add(new Setting(Datasource.DB_POOL_DRIVER, "org.h2.Driver"));
        settings.add(new Setting(Datasource.DB_POOL_URL, "jdbc:h2:file:build/dbs/testdb"));
        settings.add(new Setting(Datasource.DB_POOL_USER, "jumpmind"));
        settings.add(new Setting(Datasource.DB_POOL_PASSWORD, "jumpmind"));
        return settings;
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
        Table testTable2 = createTestTable2();
        Database database = new Database();
        database.addTable(testTable1);
        database.addTable(testTable2);
        return database;
    }

    private static Table createTestTable1() {

        Table table = new Table("test_table_1");

        List<Column> columns = new ArrayList<Column>();
        columns.add(new Column("col1", true, Types.INTEGER, 4, 1));
        columns.add(new Column("col2", false, Types.VARCHAR, 50, 50));
        columns.add(new Column("col3", false, Types.DECIMAL, 9, 2));

        table.addColumns(columns);
        return table;
    }
    
    private static Table createTestTable2() {

        Table table = new Table("test_table_2");

        List<Column> columns = new ArrayList<Column>();
        columns.add(new Column("colx", true, Types.INTEGER, 4, 1));
        columns.add(new Column("coly", false, Types.VARCHAR, 50, 50));
        columns.add(new Column("colz", false, Types.DECIMAL, 9, 2));

        table.addColumns(columns);
        return table;
    }

    private static void populateTestDatabase(IDatabasePlatform platform, Database database) {

        ISqlTemplate template = platform.getSqlTemplate();
        DmlStatement statement = platform.createDmlStatement(DmlType.INSERT,
                database.findTable("test_table_1"), null);
        template.update(statement.getSql(),
                statement.getValueArray(new Object[] { 1, "test row 1", 7.7 }, new Object[] { 1 }));
        template.update(statement.getSql(),
                statement.getValueArray(new Object[] { 2, "test row 2", 8.8 }, new Object[] { 1 }));
        template.update(statement.getSql(),
                statement.getValueArray(new Object[] { 3, "test row 3", 9.9 }, new Object[] { 1 }));
        
        statement = platform.createDmlStatement(DmlType.INSERT, database.findTable("test_table_2"),null);
        template.update(statement.getSql(),
                statement.getValueArray(new Object[] { 1, "test row x", 7.7 }, new Object[] { 1 }));
        template.update(statement.getSql(),
                statement.getValueArray(new Object[] { 2, "test row y", 8.8 }, new Object[] { 1 }));
        template.update(statement.getSql(),
                statement.getValueArray(new Object[] { 3, "test row z", 9.9 }, new Object[] { 1 }));
        
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
