package org.jumpmind.metl.core.runtime.component;

import java.io.File;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.h2.Driver;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
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

    IDatabasePlatform databasePlatform;

    File databaseFile;

    RdbmsWriter databaseWriter;

    @Override
    public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {
        createDatabase();
        String originatingStepId = inputMessage.getHeader().getOriginatingStepId();
        String tableSuffix = null;
        if (sourceStep1Id.equals(originatingStepId)) {
            tableSuffix = "_1";
        } else if (sourceStep2Id.equals(originatingStepId)) {
            tableSuffix = "_2";
        }

        if (databaseWriter == null) {
            databaseWriter.setDatabasePlatform(databasePlatform);
        }

        if (tableSuffix != null) {
            databaseWriter.setTableSuffix(tableSuffix);
            databaseWriter.handle(inputMessage, null, false);
        }
        
        if (unitOfWorkBoundaryReached) {
            // select where in _1 and not in _2 for DEL
            // select where in _2 and not in _1 for ADD
        }

    }

    protected void createDatabase() {
        if (databasePlatform == null) {
            ResettableBasicDataSource ds = new ResettableBasicDataSource();
            ds.setDriverClassName(Driver.class.getName());
            String uuid = UUID.randomUUID().toString();
            if (inMemoryCompare) {
                ds.setUrl("jdbc:h2:mem:" + uuid);
            } else {
                databaseFile = new File(System.getProperty("h2.baseDir"), uuid + ".h2.db");
                ds.setUrl("jdbc:h2:file:./" + uuid);
            }
            databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(ds, new SqlTemplateSettings(), false);

            Model inputModel = context.getFlowStep().getComponent().getInputModel();
            List<ModelEntity> entities = inputModel.getModelEntities();
            for (ModelEntity entity : entities) {
                Table table = new Table();
                table.setName(entity.getName() + "_1");
                List<ModelAttribute> attributes = entity.getModelAttributes();
                for (ModelAttribute attribute : attributes) {
                    DataType dataType = attribute.getDataType();
                    Column column = new Column(attribute.getName());
                    if (dataType.isNumeric()) {
                        column.setTypeCode(Types.DECIMAL);
                    } else if (dataType.isBoolean()) {
                        column.setTypeCode(Types.BOOLEAN);
                    } else if (dataType.isTimestamp()) {
                        column.setTypeCode(Types.TIMESTAMP);
                    } else if (dataType.isBinary()) {
                        column.setTypeCode(Types.BLOB);
                    } else {
                        column.setTypeCode(Types.LONGVARCHAR);
                    }
                    table.addColumn(column);
                }
                databasePlatform.createTables(false, false, table);

                table.setName(entity.getName() + "_2");
                databasePlatform.createTables(false, false, table);

            }

            log(LogLevel.INFO, "Creating databasePlatform with the following url: %s", ds.getUrl());
        }
    }

    @Override
    public void stop() {
        if (databaseFile != null) {
            log(LogLevel.INFO, "Deleting databasePlatform file: %s", databaseFile);
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

        Model inputModel = context.getFlowStep().getComponent().getInputModel();
        if (inputModel == null) {
            throw new MisconfiguredException("The input model is not set and it is required");
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
