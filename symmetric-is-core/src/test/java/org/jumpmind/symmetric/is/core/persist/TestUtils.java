package org.jumpmind.symmetric.is.core.persist;

import java.io.File;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.BasicDataSourcePropertyConstants;
import org.jumpmind.properties.TypedProperties;

public class TestUtils {

    public static IDatabasePlatform createDatabasePlatform() throws Exception {
        final String DB_DIR = "build/dbs";
        FileUtils.deleteDirectory(new File(DB_DIR));
        TypedProperties properties = new TypedProperties();
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_DRIVER, "org.h2.Driver");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_URL, "jdbc:h2:file:"
                + DB_DIR + "/testdb");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_USER, "jumpmind");
        properties.setProperty(BasicDataSourcePropertyConstants.DB_POOL_PASSWORD, "jumpmind");
        DataSource ds = BasicDataSourceFactory.create(properties);
        return JdbcDatabasePlatformFactory.createNewPlatformInstance(ds, new SqlTemplateSettings(),
                false);
    }
}
