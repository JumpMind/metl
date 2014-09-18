package org.jumpmind.symmetric.is.core.persist;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Types;
import java.util.Date;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.sql.ISqlTemplate;
import org.jumpmind.db.sql.Row;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class JdbcPersistenceManagerTest {

    JdbcPersistenceManager manager;
    
    Table testTableA;

    @Before
    public void setup() throws Exception {
        manager = new JdbcPersistenceManager(TestUtils.createDatabasePlatform());
        
        testTableA = new Table("A");
        testTableA.addColumn(new Column("ID", true, Types.INTEGER, -1, -1));
        testTableA.addColumn(new Column("LAST_UPDATE_TIME", false, Types.TIMESTAMP, -1, -1));
        testTableA.addColumn(new Column("NOTE", false, Types.VARCHAR, 100, -1));
        
        manager.getDatabasePlatform().alterCaseToMatchDatabaseDefaultCase(testTableA);
        manager.getDatabasePlatform().createTables(true, true, testTableA);

    }

    @After
    public void tearDown() throws Exception {
        ResettableBasicDataSource ds = manager.databasePlatform.getDataSource();
        ds.close();
    }
    
    @Test
    public void testInsert() {
        Date date = new Date();
        manager.insert(new A(1, date, "Hello"), null, null, "A");
        
        Row row = getRow(1);
        assertEquals(1, row.get("id"));
        assertEquals("Hello", row.get("note"));
        assertEquals(date, row.get("last_update_time"));
    }
    
    @Test
    public void testUpdate() {
        Date date = new Date();
        A a = new A(1, date, "Hello");
        manager.insert(a, null, null, "A");

        Row row = getRow(1);
        assertEquals("Hello", row.get("note"));

        a.setNote("Goodbye");
        manager.update(a, null, null, "A");
        
        row = getRow(1);
        assertEquals("Goodbye", row.get("note"));
        
    }
    
    @Test
    public void testSave() {
        Date date = new Date();        
        A a = new A(1, date, "Hello");
        assertTrue(manager.save(a));
        
        date = new Date();
        a.setLastUpdateTime(date);
        assertFalse(manager.save(a));
        
        Row row = getRow(1);
        assertEquals(date, row.get("last_update_time"));
    }
    
    @Test
    public void testDelete() {
        Date date = new Date();        
        A a = new A(999, date, "Hello");
        assertTrue(manager.save(a));

        assertNotNull(getRow(999));
        
        manager.delete(a);
        
        assertNull(getRow(999));

    }


    protected Row getRow(int id) {
        ISqlTemplate template = manager.getDatabasePlatform().getSqlTemplate();
        return template.queryForRow("select * from a where id=?", id);
    }

    public static class A {
        protected int id;
        protected Date lastUpdateTime;
        protected String note;

        public A(int id, Date lastUpdateTime, String note) {
            super();
            this.id = id;
            this.lastUpdateTime = lastUpdateTime;
            this.note = note;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public Date getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(Date lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }

    }

}
