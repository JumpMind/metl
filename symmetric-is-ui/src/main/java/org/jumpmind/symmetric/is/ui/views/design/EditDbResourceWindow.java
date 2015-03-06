package org.jumpmind.symmetric.is.ui.views.design;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.jumpmind.symmetric.is.core.runtime.resource.db.DataSourceResource.DB_POOL_DRIVER;
import static org.jumpmind.symmetric.is.core.runtime.resource.db.DataSourceResource.DB_POOL_PASSWORD;
import static org.jumpmind.symmetric.is.core.runtime.resource.db.DataSourceResource.DB_POOL_URL;
import static org.jumpmind.symmetric.is.core.runtime.resource.db.DataSourceResource.DB_POOL_USER;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.jumpmind.db.util.BasicDataSourceFactory;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.resource.db.DataSourceResource;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.IItemUpdatedListener;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.jumpmind.symmetric.ui.common.UiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
public class EditDbResourceWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    private static final String ASE = "Sybase ASE";
    private static final String SQLANYWHERE = "Sybase SQL Anywhere";
    private static final String INFORMIX = "Informix";
    private static final String DB2 = "DB2 UDB";
    private static final String POSTGRE_SQL = "PostgreSQL";
    private static final String GREENPLUM = "Greenplum";
    private static final String INTERBASE = "Interbase";
    private static final String FIREBIRD = "Firebird";
    private static final String SQL_SERVER_JTDS = "SQL Server";
    private static final String SQL_SERVER_MICROSOFT = "SQL Server Azure";
    private static final String H2 = "H2";
    private static final String HSQLDB = "HSQLDB";
    private static final String DERBY = "Derby";
    private static final String ORACLE = "Oracle";
    private static final String MY_SQL = "MySQL";
    private static final String SQLITE = "SQLite";
    private static final String MARIADB = "MariaDB";
    private static final String REDSHIFT = "Redshift";

    static final Map<String, String> databaseToDriverMap = new TreeMap<String, String>();

    static final Map<String, String> databaseToSampleUrlMap = new TreeMap<String, String>();

    static final Map<String, String> databaseToValidationQueryMap = new TreeMap<String, String>();

    static final Map<String, String> databaseResourceProperties = new TreeMap<String, String>();

    static final Map<String, String> databaseInitSqlProperties = new TreeMap<String, String>();

    static final Map<String, String> synchronizedJobsProperties = new TreeMap<String, String>();

    static {
        databaseToDriverMap.put(MY_SQL, "com.mysql.jdbc.Driver");
        databaseToDriverMap.put(MARIADB, "com.mysql.jdbc.Driver");
        databaseToDriverMap.put(ORACLE, "oracle.jdbc.driver.OracleDriver");
        databaseToDriverMap.put(DERBY, "org.apache.derby.jdbc.EmbeddedDriver");
        databaseToDriverMap.put(HSQLDB, "org.hsqldb.jdbcDriver");
        databaseToDriverMap.put(H2, "org.h2.Driver");
        databaseToDriverMap.put(SQL_SERVER_JTDS, "net.sourceforge.jtds.jdbc.Driver");
        databaseToDriverMap.put(SQL_SERVER_MICROSOFT,
                "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        databaseToDriverMap.put(FIREBIRD, "org.firebirdsql.jdbc.FBDriver");
        databaseToDriverMap.put(INTERBASE, "interbase.interclient.Driver");
        databaseToDriverMap.put(POSTGRE_SQL, "org.postgresql.Driver");
        databaseToDriverMap.put(GREENPLUM, "org.postgresql.Driver");
        databaseToDriverMap.put(DB2, "com.ibm.db2.jcc.DB2Driver");
        databaseToDriverMap.put(INFORMIX, "com.informix.jdbc.IfxDriver");
        databaseToDriverMap.put(ASE, "com.sybase.jdbc4.jdbc.SybDriver");
        databaseToDriverMap.put(SQLANYWHERE, "com.sybase.jdbc4.jdbc.SybDriver");
        databaseToDriverMap.put(SQLITE, "org.sqlite.JDBC");
        databaseToDriverMap.put(REDSHIFT, "org.postgresql.Driver");

        databaseToSampleUrlMap
                .put(MY_SQL,
                        "jdbc:mysql://hostname/databasename?tinyInt1isBit=false&zeroDateTimeBehavior=convertToNull");
        databaseToSampleUrlMap
                .put(MARIADB,
                        "jdbc:mysql://hostname/databasename?tinyInt1isBit=false&zeroDateTimeBehavior=convertToNull");
        databaseToSampleUrlMap.put(ORACLE, "jdbc:oracle:thin:@127.0.0.1:1521:databasename");
        databaseToSampleUrlMap.put(DERBY, "jdbc:derby:databasename;create=true");
        databaseToSampleUrlMap.put(HSQLDB, "jdbc:hsqldb:file:databasename;shutdown=true");
        databaseToSampleUrlMap.put(H2,
                "jdbc:h2:file:databasename;AUTO_SERVER=TRUE;LOCK_TIMEOUT=60000");
        databaseToSampleUrlMap
                .put(SQL_SERVER_JTDS,
                        "jdbc:jtds:sqlserver://hostname/databasename;useCursors=true;bufferMaxMemory=10240;lobBuffer=5242880;socketTimeout=300;appName=symds");
        databaseToSampleUrlMap.put(SQL_SERVER_MICROSOFT,
                "jdbc:sqlserver://hostname;databasename=databasename");

        databaseToSampleUrlMap.put(FIREBIRD,
                "jdbc:firebirdsql:hostname:/var/lib/firebird/data/databasename");
        databaseToSampleUrlMap.put(INTERBASE,
                "jdbc:interbase://hostname:3070//opt/interbase/data/databasename.gdb");
        databaseToSampleUrlMap
                .put(POSTGRE_SQL,
                        "jdbc:postgresql://hostname/databasename?protocolVersion=2&stringtype=unspecified&socketTimeout=300");
        databaseToSampleUrlMap.put(GREENPLUM, "jdbc:postgresql://hostname/databasename");
        databaseToSampleUrlMap.put(DB2, "jdbc:db2://hostname:50000/databasename");
        databaseToSampleUrlMap.put(INFORMIX,
                "jdbc:informix-sqli://hostname:9088/databasename:INFORMIXSERVER=ol_ids_1150_1");
        databaseToSampleUrlMap.put(ASE, "jdbc:sybase:Tds:hostname:5000/databasename");
        databaseToSampleUrlMap.put(SQLANYWHERE, "jdbc:sybase:Tds:hostname:2638/databasename");
        databaseToSampleUrlMap.put(SQLITE, "jdbc:sqlite:databasename");
        databaseToSampleUrlMap
                .put(REDSHIFT,
                        "jdbc:postgresql://hostname.redshift.amazonaws.com:5439/database?tcpKeepAlive=true&stringtype=unspecified&socketTimeout=300");

        databaseToValidationQueryMap.put(MY_SQL, "select 1");
        databaseToValidationQueryMap.put(MARIADB, "select 1");
        databaseToValidationQueryMap.put(ORACLE, "select 1 from dual");
        databaseToValidationQueryMap.put(DERBY, "values(1)");
        databaseToValidationQueryMap.put(HSQLDB, "select 1 from SYSTEM_USERS");
        databaseToValidationQueryMap.put(H2, "select 1");
        databaseToValidationQueryMap.put(SQL_SERVER_JTDS, "select 1");
        databaseToValidationQueryMap.put(SQL_SERVER_MICROSOFT, "select 1");
        databaseToValidationQueryMap.put(FIREBIRD, "select cast(1 AS INTEGER) from rdb$database");
        databaseToValidationQueryMap.put(INTERBASE, "select cast(1 AS INTEGER) from rdb$database");
        databaseToValidationQueryMap.put(POSTGRE_SQL, "select 1");
        databaseToValidationQueryMap.put(GREENPLUM, "select 1");
        databaseToValidationQueryMap.put(DB2, "select max(1) from syscat.datatypes");
        databaseToValidationQueryMap.put(INFORMIX, "");
        databaseToValidationQueryMap.put(ASE, "select 1");
        databaseToValidationQueryMap.put(SQLANYWHERE, "select 1");
        databaseToValidationQueryMap.put(SQLITE, "select 1");
        databaseToValidationQueryMap.put(REDSHIFT, "select 1");

        databaseResourceProperties
                .put(ORACLE,
                        "oracle.net.CONNECT_TIMEOUT=60000;oracle.net.READ_TIMEOUT=60000;oracle.jdbc.ReadTimeout=60000;SetBigStringTryClob=true");

        databaseInitSqlProperties.put(SQL_SERVER_JTDS, "SET NOCOUNT OFF");
        databaseInitSqlProperties.put(SQL_SERVER_MICROSOFT, "SET NOCOUNT OFF");

        synchronizedJobsProperties.put(SQLITE, "true");
    }

    @Autowired
    IConfigurationService configurationService;

    IItemUpdatedListener itemSavedListener;

    Resource resource;

    TextField nameField;

    AbstractSelect databaseType;

    TextField databaseUrl;

    TextField userIdField;

    PasswordField passwordField;

    public EditDbResourceWindow() {

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        VerticalLayout topLayout = new VerticalLayout();
        topLayout.setMargin(true);
        topLayout.setSizeFull();
        topLayout.addStyleName("v-scrollable");
        content.addComponent(topLayout);
        content.setExpandRatio(topLayout, 1);

        FormLayout formLayout = new FormLayout();
        topLayout.addComponent(formLayout);

        nameField = new TextField("Name");
        nameField.setWidth(UiConstants.TEXTFIELD_WIDTH, Unit.PIXELS);
        nameField.setNullRepresentation("");

        databaseType = new ComboBox("Database Type");

        databaseUrl = new TextField("Database Url");
        databaseUrl.setNullRepresentation("");
        databaseUrl.setWidth(550, Unit.PIXELS);

        userIdField = new TextField("Username");
        userIdField.setNullRepresentation("");
        passwordField = new PasswordField("Password");
        passwordField.setNullRepresentation("");

        for (String databaseName : databaseToDriverMap.keySet()) {
            databaseType.addItem(databaseName);
        }
        databaseType.setNewItemsAllowed(false);
        databaseType.setNullSelectionAllowed(false);
        databaseType.setImmediate(true);
        databaseType.addValueChangeListener(new Property.ValueChangeListener() {
            private static final long serialVersionUID = 1L;

            public void valueChange(ValueChangeEvent event) {
                String url = databaseToSampleUrlMap.get(databaseType.getValue());
                databaseUrl.setValue(url);
                final String DATABASENAME = "databasename";
                int index = url.lastIndexOf(DATABASENAME);
                if (index > 0) {
                    databaseUrl.setSelectionRange(index, DATABASENAME.length());
                }
            }
        });

        formLayout.addComponent(nameField);
        formLayout.addComponent(databaseType);
        formLayout.addComponent(databaseUrl);
        formLayout.addComponent(userIdField);
        formLayout.addComponent(passwordField);

        Button testButton = new Button("Test");
        testButton.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                testResource(true, true);
            }
        });

        Button cancelButton = new Button("Cancel", new CloseButtonListener());
        Button saveButton = new Button("Save", new SaveClickListener());
        saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);

        content.addComponent(buildButtonFooter(new Button[] { testButton }, new Button[] {
                cancelButton, saveButton }));

        databaseType.focus();

    }

    public void show(Resource resource, IItemUpdatedListener itemSavedListener) {

        this.resource = resource;

        this.itemSavedListener = itemSavedListener;

        nameField.setValue(resource.getName());
                
        databaseType.setValue(findDatabaseType());
        databaseUrl.setValue(resource.get(DB_POOL_URL));
        userIdField.setValue(resource.get(DB_POOL_USER));
        passwordField.setValue(resource.get(DB_POOL_PASSWORD));

        setCaption("Edit Resource");

        showAtSize(.6);

        nameField.focus();

    }
    
    protected String findDatabaseType() {
        String driver = resource.get(DB_POOL_DRIVER);
        if (driver != null) {
            Set<Map.Entry<String, String>> entries = databaseToDriverMap.entrySet();
            for (Map.Entry<String, String> entry : entries) {
                if (entry.getValue().equals(driver)) {
                    return entry.getKey();
                }
            }
        }
        return "";
    }

    public Resource getResource() {
        return resource;
    }

    protected boolean testResource(boolean showSuccessNotification,
            boolean showFailureNotification) {
        String databaseName = (String) databaseType.getValue();
        try {
            if (!databaseUrl.getValue().toString().startsWith("jdbc:")) {
                CommonUiUtils.notify("Incorrect URL format", Type.WARNING_MESSAGE);
                return false;
            } else if (StringUtils.isBlank(databaseName)) {
                CommonUiUtils.notify("Please choose a database type", Type.WARNING_MESSAGE);
                return false;
            } else {
                BasicDataSourceFactory.prepareDriver(databaseToDriverMap.get(databaseName));
                ResettableBasicDataSource ds = new ResettableBasicDataSource();
                ds.setInitialSize(1);
                ds.setMaxActive(1);
                ds.setDriverClassName(databaseToDriverMap.get(databaseName));
                ds.setUrl((String) databaseUrl.getValue());
                ds.setUsername((String) userIdField.getValue());
                ds.setPassword((String) passwordField.getValue());
                java.sql.Connection c = ds.getConnection();
                c.close();
                ds.close();
                if (showSuccessNotification) {
                    CommonUiUtils.notify("Connection Successful", Type.HUMANIZED_MESSAGE);
                }
                return true;
            }
        } catch (Exception ex) {
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if (rootCause == null) {
                rootCause = ex;
            }
            if (showFailureNotification) {
                if (rootCause.getMessage().startsWith("No suitable driver")
                        || rootCause instanceof ClassNotFoundException) {
                    CommonUiUtils.notify(
                            "Please check that the database driver is correctly installed",
                            Type.WARNING_MESSAGE);
                } else {
                    CommonUiUtils.notify(rootCause.getMessage(), Type.WARNING_MESSAGE);
                    log.warn(
                            "Unsuccessful attempt to connect to a database using url: "
                                    + databaseUrl.getValue() + " with driver: "
                                    + databaseToDriverMap.get(databaseName) + " as user: "
                                    + userIdField.getValue(), rootCause);
                }
            }
            return false;
        }
    }

    protected void save() {
        resource.setType(DataSourceResource.TYPE);
        if (isBlank(nameField.getValue())) {
            CommonUiUtils.notify("The name of the resource cannot be blank", Type.WARNING_MESSAGE);
            return;
        }
        
        

        String type = (String)databaseType.getValue();
        resource.setName(nameField.getValue());
        resource.put(DB_POOL_URL, databaseUrl.getValue());
        resource.put(DB_POOL_DRIVER, databaseToDriverMap.get(type));
        resource.put(DB_POOL_USER, userIdField.getValue());
        resource.put(DB_POOL_PASSWORD, passwordField.getValue());
        
        configurationService.save(resource);

        // do some validation
        itemSavedListener.itemUpdated(resource);
        close();
    }

    class SaveClickListener implements ClickListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void buttonClick(ClickEvent event) {
            save();
        }

    }

}
