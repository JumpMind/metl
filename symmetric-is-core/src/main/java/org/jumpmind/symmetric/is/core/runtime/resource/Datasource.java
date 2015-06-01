package org.jumpmind.symmetric.is.core.runtime.resource;

import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;

@ResourceDefinition(typeName=Datasource.TYPE, resourceCategory=ResourceCategory.DATASOURCE)
public class Datasource extends AbstractResourceRuntime implements IResourceRuntime {

    public static final String TYPE = "Database";
    
    @SettingDefinition(order = 0, required = true, type = Type.TEXT, defaultValue = "org.h2.Driver", label = "Driver")
    public final static String DB_POOL_DRIVER = "db.driver";

    @SettingDefinition(order = 10, required = true, type = Type.TEXT, defaultValue = "jdbc:h2:mem:db", label = "Url")
    public final static String DB_POOL_URL = "db.url";

    @SettingDefinition(order = 20, type = Type.TEXT, label = "User")
    public final static String DB_POOL_USER = "db.user";

    @SettingDefinition(order = 30, type = Type.PASSWORD, label = "Password")
    public final static String DB_POOL_PASSWORD = "db.password";

    @SettingDefinition(order = 40, type = Type.TEXT, label = "Validation Query")
    public final static String DB_POOL_VALIDATION_QUERY = "db.validation.query";

    @SettingDefinition(order = 50, type = Type.INTEGER, defaultValue = "5", label = "Initial Size")
    public final static String DB_POOL_INITIAL_SIZE = "db.pool.initial.size";

    @SettingDefinition(order = 60, type = Type.INTEGER, defaultValue = "20", label = "Max Active")
    public final static String DB_POOL_MAX_ACTIVE = "db.pool.max.active";

    @SettingDefinition(order = 70, type = Type.INTEGER, defaultValue = "10", label = "Max Idle")
    public final static String DB_POOL_MAX_IDLE = "db.pool.max.idle";

    @SettingDefinition(order = 80, type = Type.INTEGER, defaultValue = "5", label = "Min Idle")
    public final static String DB_POOL_MIN_IDLE = "db.pool.min.idle";

    @SettingDefinition(order = 90, type = Type.INTEGER, defaultValue = "30000", label = "Wait Time (ms)")
    public final static String DB_POOL_MAX_WAIT = "db.pool.max.wait.millis";

    @SettingDefinition(order = 100, type = Type.INTEGER, defaultValue = "120000", label = "Evict Time (ms)")
    public final static String DB_POOL_MIN_EVICTABLE_IDLE_TIME_MILLIS = "db.pool.min.evictable.idle.millis";

    @SettingDefinition(order = 110, type = Type.BOOLEAN, defaultValue = "false", label = "Test on Borrow")
    public final static String DB_POOL_TEST_ON_BORROW = "db.test.on.borrow";

    @SettingDefinition(order = 120, type = Type.BOOLEAN, defaultValue = "false", label = "Test on Return")
    public final static String DB_POOL_TEST_ON_RETURN = "db.test.on.return";

    @SettingDefinition(order = 130, type = Type.BOOLEAN, defaultValue = "true", label = "Test while Idle")
    public final static String DB_POOL_TEST_WHILE_IDLE = "db.test.while.idle";

    @SettingDefinition(order = 140, type = Type.TEXT, label = "Init Sql")
    public final static String DB_POOL_INIT_SQL = "db.init.sql";

    @SettingDefinition(order = 150, type = Type.TEXT, label = "Resource Properties")
    public final static String DB_POOL_RESOURCE_PROPERTIES = "db.resource.properties";

    @SettingDefinition(order = 170, type = Type.INTEGER, defaultValue = "1000", label = "Fetch Size")
    public final static String DB_FETCH_SIZE = "db.fetch.size";

    @SettingDefinition(order = 180, type = Type.INTEGER, defaultValue = "300", label = "Query Timeout (seconds)")
    public final static String DB_QUERY_TIMEOUT = "db.query.timeout";
    
    @SettingDefinition(order = 170, type = Type.INTEGER, defaultValue = "" + Integer.MAX_VALUE, label = "Max Rows To Select")
    public final static String DB_MAX_ROWS = "db.max.rows.to.select";
    
    @SettingDefinition(order = 200, type = Type.CHOICE, choices = { "TYPE_FORWARD_ONLY",
            "TYPE_SCROLL_INSENSITIVE", "TYPE_SCROLL_SENSITIVE" }, defaultValue = "TYPE_FORWARD_ONLY", label = "ResultSet Type")
    public final static String DB_RESULTSET_TYPE = "db.resultset.type";

    @SettingDefinition(order = 210, type = Type.CHOICE, choices = { "CONCUR_READ_ONLY",
            "CONCUR_UPDATABLE" }, defaultValue = "CONCUR_READ_ONLY", label = "ResultSet Concurrency")
    public final static String DB_RESULTSET_CONCURRENCY = "db.resultset.concurrency";

    @SettingDefinition(order = 220, type = Type.CHOICE, choices = { "HOLD_CURSORS_OVER_COMMIT",
            "CLOSE_CURSORS_AT_COMMIT" }, defaultValue = "HOLD_CURSORS_OVER_COMMIT", label = "ResultSet Holdability")
    public final static String DB_RESULTSET_HOLDABILITY = "db.resultset.holdability";

    ResettableBasicDataSource dataSource = new ResettableBasicDataSource();

    
    @Override
    protected void start(TypedProperties properties) {
        this.dataSource = BasicDataSourceFactory
                .create(properties);
    }

    @Override
    public void stop() {
        try {
            dataSource.close();
        } catch (Exception e) {
        } finally {
            dataSource = null;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T)dataSource;
    }
    

}
