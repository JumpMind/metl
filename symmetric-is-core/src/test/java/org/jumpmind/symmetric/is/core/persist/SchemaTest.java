package org.jumpmind.symmetric.is.core.persist;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.jumpmind.db.io.DatabaseXmlUtil;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.BasicDataSourcePropertyConstants;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.properties.TypedProperties;
import org.junit.Before;
import org.junit.Test;

public class SchemaTest {

    IDatabasePlatform platform;
    ResettableBasicDataSource ds;

    @Before
    public void setup() throws Exception {
        final String DB_DIR = "build/dbs";
        if (ds != null) {
            ds.close();
        }
        FileUtils.deleteDirectory(new File(DB_DIR));
        TypedProperties properties = new TypedProperties();
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_DRIVER, "org.h2.Driver");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_URL, "jdbc:h2:file:"
                + DB_DIR + "/testdb");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_USER, "jumpmind");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_PASSWORD, "jumpmind");
        ds = BasicDataSourceFactory.create(properties);
        platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(ds,
                new SqlTemplateSettings(), false);
    }

    @Test
    public void createSchema() throws Exception {
        Database db = DatabaseXmlUtil.read(getClass().getResourceAsStream("/schema-v1.xml"));
        platform.createDatabase(db, false, false);
    }
}
