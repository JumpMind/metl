package org.jumpmind.symmetric.is.core.persist;

import org.jumpmind.db.io.DatabaseXmlUtil;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SchemaTest {

    IDatabasePlatform platform;

    @Before
    public void setup() throws Exception {
        platform = TestUtils.createDatabasePlatform();
    }
    
    @After
    public void tearDown() throws Exception {
        ResettableBasicDataSource ds = platform.getDataSource();
        ds.close();
    }

    @Test
    public void createSchema() throws Exception {
        Database db = DatabaseXmlUtil.read(getClass().getResourceAsStream("/schema-v1.xml"));
        platform.createDatabase(db, false, false);
    }
}
