package org.jumpmind.metl.core.runtime.component;

import java.io.File;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.h2.Driver;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class DataDiff extends AbstractComponentRuntime {

    public static String SOURCE_1 = "source.1";
    public static String SOURCE_2 = "source.2";
    public static String IN_MEMORY_COMPARE = "in.memory.compare";

    int rowsPerMessage = 10000;

    String sourceStep1Id;

    String sourceStep2Id;

    boolean inMemoryCompare = true;    

    IDatabasePlatform database;

    File databaseFile;

    @Override
    public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {
        createDatabase();
    }

    protected void createDatabase() {
        if (database == null) {
            ResettableBasicDataSource ds = new ResettableBasicDataSource();
            ds.setDriverClassName(Driver.class.getName());
            String uuid = UUID.randomUUID().toString();
            if (inMemoryCompare) {
                ds.setUrl("jdbc:h2:mem:" + uuid);
            } else {
                databaseFile = new File(System.getProperty("h2.baseDir"), uuid + ".h2.db");
                ds.setUrl("jdbc:h2:file:./" + uuid);
            }
            database = JdbcDatabasePlatformFactory.createNewPlatformInstance(ds, new SqlTemplateSettings(), false);

            log(LogLevel.INFO, "Creating databse with the following url: %s", ds.getUrl());
        }
    }
    
    @Override
    public void stop() {
        if (databaseFile != null) {
            FileUtils.deleteQuietly(databaseFile);
        }
    }

    @Override
    protected void start() {
        TypedProperties properties = getTypedProperties();
        this.sourceStep1Id = properties.get(SOURCE_1);
        this.sourceStep2Id = properties.get(SOURCE_2);
        this.inMemoryCompare = properties.is(IN_MEMORY_COMPARE);
        this.rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
