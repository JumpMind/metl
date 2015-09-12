package org.jumpmind.metl.core.persist;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.util.ConfigDatabaseUpgrader;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.metl.core.utils.DbTestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class SchemaTest {

    IDatabasePlatform platform;

    @Before
    public void setup() throws Exception {
        platform = DbTestUtils.createDatabasePlatform();
    }
    
    @After
    public void tearDown() throws Exception {
        ResettableBasicDataSource ds = platform.getDataSource();
        ds.close();
    }

    @Test
    public void createSchema() throws Exception {        
        ConfigDatabaseUpgrader upgrader = new ConfigDatabaseUpgrader("/schema-v1.xml", platform, true, "SIS_");
        upgrader.upgrade();
    }
}
