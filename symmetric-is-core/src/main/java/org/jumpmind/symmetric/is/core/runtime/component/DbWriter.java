package org.jumpmind.symmetric.is.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.SqlException;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.sql.UniqueKeyException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.util.FormatUtils;

@ComponentDefinition(
        typeName = DbWriter.TYPE,
        category = ComponentCategory.WRITER,
        iconImage = "dbwriter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.NONE,
        resourceCategory = ResourceCategory.DATASOURCE)
public class DbWriter extends AbstractComponentRuntime {

    public static final String TYPE = "Database Writer";

    @SettingDefinition(order = 5, required = false, type = Type.TEXT, label = "Catalog")
    public final static String CATALOG = "catalog";

    @SettingDefinition(order = 6, required = false, type = Type.TEXT, label = "Schema")
    public final static String SCHEMA = "schema";

    @SettingDefinition(order = 10, required = false, type = Type.BOOLEAN, label = "Replace rows if they exist", defaultValue = "false")
    public final static String REPLACE = "db.writer.replace";

    @SettingDefinition(
            order = 20,
            required = false,
            type = Type.BOOLEAN,
            label = "Update rows first instead of insert",
            defaultValue = "false")
    public final static String UPDATE_FIRST = "db.writer.update.first";

    @SettingDefinition(
            order = 30,
            required = false,
            type = Type.BOOLEAN,
            label = "Fallback to insert if no rows updated",
            defaultValue = "false")
    public final static String INSERT_FALLBACK = "db.writer.insert.fallback";

    @SettingDefinition(order = 40, required = false, type = Type.BOOLEAN, label = "Quote table and column names", defaultValue = "false")
    public final static String QUOTE_IDENTIFIERS = "db.writer.quote.identifiers";

    @SettingDefinition(
            order = 50,
            required = false,
            type = Type.BOOLEAN,
            label = "Trim character data to fit within column",
            defaultValue = "false")
    public final static String FIT_TO_COLUMN = "db.writer.fit.to.column";

    @SettingDefinition(order = 60, required = false, type = Type.BOOLEAN, label = "Stop Processing Msgs on Error", defaultValue = "true")
    public final static String STOP_PROCESSING_ON_ERROR = "stop.processing.on.error";

    public final static String ATTRIBUTE_INSERT_ENABLED = "insert.enabled";

    public final static String ATTRIBUTE_UPDATE_ENABLED = "update.enabled";

    public final static String BATCH_MODE = "batch.mode";

    boolean replaceRows = false;

    boolean updateFirst = false;

    boolean insertFallback = false;

    boolean quoteIdentifiers = false;

    boolean fitToColumn = false;

    boolean stopProcessingOnError = true;

    String catalogName;

    String schemaName;

    int inboundEntityDataCount = 0;

    boolean batchMode = false;

    IDatabasePlatform platform;

    List<TargetTableDefintion> targetTables;

    Throwable error;

    String lastPreparedDml;

    @Override
    protected void start() {
        inboundEntityDataCount = 0;
        error = null;

        if (getResourceRuntime() == null) {
            throw new IllegalStateException("A database writer must have a datasource defined");
        }

        Model model = getInputModel();
        if (model == null) {
            throw new IllegalStateException("A database writer must have an input model defined");
        }

        TypedProperties properties = getComponent().toTypedProperties(getSettingDefinitions(false));
        batchMode = properties.is(BATCH_MODE, batchMode);
        replaceRows = properties.is(REPLACE);
        updateFirst = properties.is(UPDATE_FIRST);
        insertFallback = properties.is(INSERT_FALLBACK);
        quoteIdentifiers = properties.is(QUOTE_IDENTIFIERS);
        stopProcessingOnError = properties.is(STOP_PROCESSING_ON_ERROR, stopProcessingOnError);
        fitToColumn = properties.is(FIT_TO_COLUMN);

        catalogName = FormatUtils.replaceTokens(properties.get(CATALOG), context.getFlowParametersAsString(), true);
        if (isBlank(catalogName)) {
            catalogName = null;
        }

        schemaName = FormatUtils.replaceTokens(properties.get(SCHEMA), context.getFlowParametersAsString(), true);
        if (isBlank(schemaName)) {
            schemaName = null;
        }

        DataSource dataSource = (DataSource) getResourceReference();
        platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource, new SqlTemplateSettings(), quoteIdentifiers);
        targetTables = new ArrayList<TargetTableDefintion>();

        for (ModelEntity entity : model.getModelEntities()) {
            Table table = platform.getTableFromCache(catalogName, schemaName, entity.getName(), true);
            if (table != null) {
                targetTables.add(new TargetTableDefintion(entity, new TargetTable(DmlType.UPDATE, entity, table.copy()), new TargetTable(
                        DmlType.INSERT, entity, table.copy())));
            }
        }
    }

    @Override
    public void handle(final Message inputMessage, final IMessageTarget messageTarget) {

        lastPreparedDml = null;
        getComponentStatistics().incrementInboundMessages();

        if (error == null || !stopProcessingOnError) {
            if (getResourceRuntime() == null) {
                throw new RuntimeException("The data source resource has not been configured.  Please configure it.");
            }

            ArrayList<EntityData> inputRows = inputMessage.getPayload();
            if (inputRows == null) {
                return;
            }

            ISqlTransaction transaction = platform.getSqlTemplate().startSqlTransaction();
            transaction.setInBatchMode(batchMode);
            try {
                write(transaction, inputRows);
                transaction.commit();
            } catch (Throwable ex) {
                error = ex;
                transaction.rollback();
                if (ex instanceof RuntimeException) {
                    throw (RuntimeException) ex;
                } else {
                    throw new RuntimeException(ex);
                }
            } finally {
                transaction.close();
            }
        }
    }

    private Object[] getValues(boolean isUpdate, TargetTable modelTable, EntityData inputRow) {
        ArrayList<Object> data = new ArrayList<Object>();
        for (TargetColumn modelColumn : modelTable.getTargetColumns()) {
            if ((isUpdate && modelColumn.isUpdateEnabled()) || (!isUpdate && modelColumn.isInsertEnabled())) {
                Object value = inputRow.get(modelColumn.getModelAttribute().getId());
                if (fitToColumn && value != null && value instanceof String) {
                    value = fitToColumn(modelTable.getTable(), modelColumn.getModelAttribute().getName(), (String) value);
                }
                data.add(value);
            }
        }

        ArrayList<Object> keyValues = new ArrayList<Object>();
        for (TargetColumn modelColumn : modelTable.getKeyTargetColumns()) {
            if ((isUpdate && modelColumn.isUpdateEnabled()) || (!isUpdate && modelColumn.isInsertEnabled())) {
                keyValues.add(inputRow.get(modelColumn.getModelAttribute().getId()));
            }
        }
        return modelTable.getStatement()
                .getValueArray(data.toArray(new Object[data.size()]), keyValues.toArray(new Object[keyValues.size()]));
    }

    private void write(ISqlTransaction transaction, List<EntityData> inputRows) {
        long ts = System.currentTimeMillis();
        int totalStatementCount = 0;
        TargetTable modelTable = null;
        Object[] data = null;
        try {
            Map<TargetTableDefintion, WriteStats> statsMap = new HashMap<TargetTableDefintion, WriteStats>();
            for (TargetTableDefintion targetTableDefinition : targetTables) {
                for (EntityData inputRow : inputRows) {
                    inboundEntityDataCount++;
                    WriteStats stats = statsMap.get(targetTableDefinition);
                    if (stats == null) {
                        stats = new WriteStats();
                        statsMap.put(targetTableDefinition, stats);
                    }
                    if (updateFirst) {
                        modelTable = targetTableDefinition.getUpdateTable();
                        if (modelTable.shouldProcess(inputRow)) {
                            data = getValues(true, modelTable, inputRow);
                            int count = execute(transaction, modelTable.getStatement(), new Object(), data, true);
                            totalStatementCount++;
                            stats.updateCount += count;
                            getComponentStatistics().incrementNumberEntitiesProcessed(count);
                            if (insertFallback && count == 0) {
                                modelTable = targetTableDefinition.getInsertTable();
                                if (modelTable.shouldProcess(inputRow)) {
                                    log.debug("Falling back to insert");
                                    data = getValues(false, modelTable, inputRow);
                                    count = execute(transaction, modelTable.getStatement(), new Object(), data, true);
                                    totalStatementCount++;
                                    stats.fallbackInsertCount += count;
                                    getComponentStatistics().incrementNumberEntitiesProcessed(count);
                                }
                            } else if (count == 0) {
                                log(LogLevel.DEBUG, String.format("Failed to update row: \n%s\nWith values: \n%s\nWith types: \n%s\n",
                                        modelTable.getStatement().getSql(), Arrays.toString(data),
                                        Arrays.toString(modelTable.getStatement().getTypes())));
                            }
                        }
                    } else {
                        try {
                            modelTable = targetTableDefinition.getInsertTable();
                            if (modelTable.shouldProcess(inputRow)) {
                                data = getValues(false, modelTable, inputRow);
                                int count = execute(transaction, modelTable.getStatement(), new Object(), data, !replaceRows);
                                totalStatementCount++;
                                stats.insertCount += count;
                                getComponentStatistics().incrementNumberEntitiesProcessed(count);
                            }
                        } catch (UniqueKeyException e) {
                            if (replaceRows) {
                                modelTable = targetTableDefinition.getUpdateTable();
                                if (modelTable.shouldProcess(inputRow)) {
                                    log.debug("Falling back to update");
                                    data = getValues(true, modelTable, inputRow);
                                    int count = execute(transaction, modelTable.getStatement(), new Object(), data, true);
                                    totalStatementCount++;
                                    stats.fallbackUpdateCount += count;
                                    getComponentStatistics().incrementNumberEntitiesProcessed(count);
                                }
                            } else {
                                throw e;
                            }
                        }
                    }
                }
            }

            info("Ran a total of %d statements in %s", totalStatementCount, formatDuration(System.currentTimeMillis() - ts));

            for (TargetTableDefintion table : targetTables) {
                WriteStats stats = statsMap.get(table);
                if (stats != null) {
                    StringBuilder msg = new StringBuilder();
                    if (stats.insertCount > 0) {
                        msg.append("Inserted: ");
                        msg.append(stats.insertCount);
                    }

                    if (stats.fallbackUpdateCount > 0) {
                        if (msg.length() > 0) {
                            msg.append(", ");
                        }
                        msg.append("Fallback Updates: ");
                        msg.append(stats.fallbackUpdateCount);
                    }

                    if (stats.updateCount > 0) {
                        if (msg.length() > 0) {
                            msg.append(", ");
                        }
                        msg.append(" Updated: ");
                        msg.append(stats.updateCount);
                    }

                    if (stats.fallbackInsertCount > 0) {
                        if (msg.length() > 0) {
                            msg.append(", ");
                        }
                        msg.append(" Fallback Inserts: ");
                        msg.append(stats.fallbackInsertCount);
                    }

                    if (msg.length() > 0) {
                        log(LogLevel.INFO, "%s: %s", table.getInsertTable().getTable().getFullyQualifiedTableName(), msg.toString());
                    }

                }
            }

        } catch (RuntimeException ex) {
            if (modelTable != null && data != null) {
                log(LogLevel.ERROR, String.format(
                        "Failed to run dml for the %s statement processed: \n%s\nWith values: \n%s\nWith types: \n%s\n",
                        inboundEntityDataCount, modelTable.getStatement().getSql(), Arrays.toString(data),
                        Arrays.toString(modelTable.getStatement().getTypes())));
            }
            throw ex;
        }

    }

    private int execute(ISqlTransaction transaction, DmlStatement dmlStatement, Object marker, Object[] data, boolean logFailure) {

        String sql = dmlStatement.getSql();

        if (!sql.equals(lastPreparedDml)) {
            transaction.flush();
            if (log.isDebugEnabled()) {
                log.debug("Preparing dml: {}", sql);
            }
            transaction.prepare(sql);
            lastPreparedDml = sql;
        }

        if (log.isDebugEnabled()) {
            log.debug("Submitting data {} with types {}", Arrays.toString(data), Arrays.toString(dmlStatement.getTypes()));
        }

        try {
            return transaction.addRow(marker, data, dmlStatement.getTypes());
        } catch (SqlException ex) {
            if (logFailure) {
                log(LogLevel.WARN, String.format("Failed to run the following sql: \n%s\nWith values: \n%s\nWith types: \n%s\n",
                        dmlStatement.getSql(), Arrays.toString(data), Arrays.toString(dmlStatement.getTypes())));
            }
            throw ex;
        }
    }

    private String fitToColumn(Table table, String columnName, String value) {
        Column column = table.findColumn(columnName);
        if (column != null) {
            int size = column.getSizeAsInt();
            if (size > 0 && value.length() > size) {
                value = value.substring(0, size);
            }
        }
        return value;
    }

    private String formatDuration(long timeInMs) {
        if (timeInMs > 60000) {
            long minutes = timeInMs / 60000;
            long seconds = (timeInMs - (minutes * 60000)) / 1000;
            return minutes + " m " + seconds + " s";
        } else if (timeInMs > 1000) {
            long seconds = timeInMs / 1000;
            return seconds + " s";
        } else {
            return timeInMs + " ms";
        }
    }

    class TargetTableDefintion {

        ModelEntity modelEntity;

        TargetTable updateTable;

        TargetTable insertTable;

        public TargetTableDefintion(ModelEntity modelEntity, TargetTable updateTable, TargetTable insertTable) {
            super();
            this.modelEntity = modelEntity;
            this.updateTable = updateTable;
            this.insertTable = insertTable;
        }

        public TargetTable getInsertTable() {
            return insertTable;
        }

        public ModelEntity getModelEntity() {
            return modelEntity;
        }

        public TargetTable getUpdateTable() {
            return updateTable;
        }

    }

    class TargetTable {

        Table table;

        DmlStatement statement;

        List<TargetColumn> keyTargetColumns = new ArrayList<TargetColumn>();

        List<TargetColumn> targetColumns = new ArrayList<TargetColumn>();

        public TargetTable(DmlType dmlType, ModelEntity entity, Table table) {
            this.table = table;
            List<ModelAttribute> attributes = entity.getModelAttributes();
            String[] columnNames = table.getColumnNames();

            /*
             * Remove columns that don't exist in the model
             */
            for (String columnName : columnNames) {
                boolean foundIt = false;
                for (ModelAttribute attribute : attributes) {
                    if (columnName.equalsIgnoreCase(attribute.getName())) {
                        foundIt = true;
                        break;
                    }
                }
                if (!foundIt) {
                    table.removeColumn(table.findColumn(columnName));
                }
            }

            /*
             * Remove columns that are not enabled for this dml type
             */
            for (ModelAttribute attribute : attributes) {
                ComponentAttributeSetting setting = getComponent().getSingleAttributeSetting(attribute.getId(),
                        dmlType == DmlType.INSERT ? ATTRIBUTE_INSERT_ENABLED : ATTRIBUTE_UPDATE_ENABLED);
                if (setting != null && !Boolean.parseBoolean(setting.getValue())) {
                    table.removeColumn(table.findColumn(attribute.getName()));
                }
            }

            statement = platform.createDmlStatement(dmlType, table, null);

            for (Column column : table.getColumns()) {
                ModelAttribute attr = entity.getModelAttributeByName(column.getName());
                if (attr != null) {
                    if (column.isPrimaryKey()) {
                        keyTargetColumns.add(new TargetColumn(attr, column));
                    }
                    targetColumns.add(new TargetColumn(attr, column));
                }
            }

        }

        public DmlStatement getStatement() {
            return statement;
        }

        public void setTable(Table table) {
            this.table = table;
        }

        public Table getTable() {
            return table;
        }

        public List<TargetColumn> getTargetColumns() {
            return targetColumns;
        }

        public List<TargetColumn> getKeyTargetColumns() {
            return keyTargetColumns;
        }

        public boolean shouldProcess(EntityData entityData) {
            for (TargetColumn targetColumn : targetColumns) {
                if (entityData.containsKey(targetColumn.getModelAttribute().getId())) {
                    return true;
                }
            }
            return false;
        }
    }

    class TargetColumn {

        ModelAttribute modelAttribute;

        Column column;

        boolean insertEnabled = true;

        boolean updateEnabled = true;

        TargetColumn(ModelAttribute modelAttribute, Column column) {
            this.modelAttribute = modelAttribute;
            this.column = column;
            ComponentAttributeSetting insertAttr = getComponent().getSingleAttributeSetting(modelAttribute.getId(), ATTRIBUTE_INSERT_ENABLED);
            insertEnabled = insertAttr != null ? Boolean.parseBoolean(insertAttr.getValue()) : true;

            ComponentAttributeSetting updateAttr = getComponent().getSingleAttributeSetting(modelAttribute.getId(), ATTRIBUTE_UPDATE_ENABLED);
            updateEnabled = updateAttr != null ? Boolean.parseBoolean(updateAttr.getValue()) : true;

        }

        public ModelAttribute getModelAttribute() {
            return modelAttribute;
        }

        public Column getColumn() {
            return column;
        }

        public boolean isInsertEnabled() {
            return insertEnabled;
        }

        public boolean isUpdateEnabled() {
            return updateEnabled;
        }
    }

    class WriteStats {
        int insertCount;
        int updateCount;
        int fallbackInsertCount;
        int fallbackUpdateCount;
    }
}
