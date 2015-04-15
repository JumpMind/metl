package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.sql.UniqueKeyException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.io.data.writer.DatabaseWriterSettings;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;

@ComponentDefinition(
        typeName = DbWriter.TYPE,
        category = ComponentCategory.WRITER,
        iconImage = "dbwriter.png",
        inputMessage = MessageType.ENTITY,
        outgoingMessage = MessageType.NONE,
        resourceCategory = ResourceCategory.DATASOURCE)
public class DbWriter extends AbstractComponent {

    public static final String TYPE = "Database Writer";

    @SettingDefinition(
            order = 1,
            required = false,
            type = Type.BOOLEAN,
            label = "Replace rows if they exist",
            defaultValue = "false")
    public final static String REPLACE = "db.writer.replace";

    @SettingDefinition(
            order = 2,
            required = false,
            type = Type.BOOLEAN,
            label = "Update rows first instead of insert",
            defaultValue = "false")
    public final static String UPDATE_FIRST = "db.writer.update.first";

    @SettingDefinition(
            order = 3,
            required = false,
            type = Type.BOOLEAN,
            label = "Fallback to insert if no rows updated",
            defaultValue = "false")
    public final static String INSERT_FALLBACK = "db.writer.insert.fallback";

    @SettingDefinition(
            order = 4,
            required = false,
            type = Type.BOOLEAN,
            label = "Quote table and column names",
            defaultValue = "false")
    public final static String QUOTE_IDENTIFIERS = "db.writer.quote.identifiers";

    @SettingDefinition(
            order = 5,
            required = false,
            type = Type.BOOLEAN,
            label = "Trim character data to fit within column",
            defaultValue = "false")
    public final static String FIT_TO_COLUMN = "db.writer.fit.to.column";

    boolean replaceRows = false;

    boolean updateFirst = false;

    boolean insertFallback = false;

    boolean quoteIdentifiers = false;

    boolean fitToColumn = false;

    IDatabasePlatform platform;

    List<TargetTable> targetTables;

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        TypedProperties properties = flowStep.getComponent().toTypedProperties(this, false);
        replaceRows = properties.is(REPLACE);
        updateFirst = properties.is(UPDATE_FIRST);
        insertFallback = properties.is(INSERT_FALLBACK);
        quoteIdentifiers = properties.is(QUOTE_IDENTIFIERS);
        fitToColumn = properties.is(FIT_TO_COLUMN);

        DataSource dataSource = (DataSource) resource.reference();
        DatabaseWriterSettings writerSettings = new DatabaseWriterSettings();
        platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource,
                new SqlTemplateSettings(), quoteIdentifiers);
        targetTables = new ArrayList<TargetTable>();

        for (ModelEntity entity : flowStep.getComponent().getInputModel().getModelEntities()) {
            Table copiedTable = platform.getTableFromCache(entity.getName(), true).copy();
            DmlStatement insert = platform.createDmlStatement(DmlType.INSERT, copiedTable,
                    writerSettings.getTextColumnExpression());
            DmlStatement update = platform.createDmlStatement(DmlType.UPDATE, copiedTable,
                    writerSettings.getTextColumnExpression());
            TargetTable targetTable = new TargetTable(entity, copiedTable, insert, update);
            targetTables.add(targetTable);

            for (Column column : copiedTable.getColumns()) {
                ModelAttribute attr = entity.getModelAttributeByName(column.getName());
                if (attr != null) {
                    if (column.isPrimaryKey()) {
                        targetTable.getKeyTargetColumns().add(new TargetColumn(attr, column));
                    }
                    targetTable.getTargetColumns().add(new TargetColumn(attr, column));
                } else {
                    copiedTable.removeColumn(column);
                }
            }
        }
    }

    @Override
    public void handle(String executionId, final Message inputMessage,
            final IMessageTarget messageTarget) {

        componentStatistics.incrementInboundMessages();

        if (resource == null) {
            throw new RuntimeException(
                    "The data source resource has not been configured.  Please configure it.");
        }

        ArrayList<EntityData> inputRows = inputMessage.getPayload();
        if (inputRows == null) {
            return;
        }

        ISqlTransaction transaction = platform.getSqlTemplate().startSqlTransaction();
        try {

            for (EntityData inputRow : inputRows) {
                for (TargetTable modelTable : targetTables) {
                    if (modelTable.shouldProcess(inputRow)) {
                        ArrayList<Object> data = new ArrayList<Object>();
                        for (TargetColumn modelColumn : modelTable.getTargetColumns()) {
                            Object value = inputRow.get(modelColumn.getModelAttribute().getId());
                            if (fitToColumn && value != null && value instanceof String) {
                                value = fitToColumn(modelTable.getTable(), modelColumn
                                        .getModelAttribute().getName(), (String) value);
                            }
                            data.add(value);
                        }
                        if (updateFirst) {
                            for (TargetColumn modelColumn : modelTable.getKeyTargetColumns()) {
                                data.add(inputRow.get(modelColumn.getModelAttribute().getId()));
                            }
                            int count = execute(transaction, modelTable.getUpdateStatement(),
                                    new Object(), data);

                            if (insertFallback && count == 0) {
                                log.debug("Falling back to insert");
                                int endIndex = data.size()
                                        - modelTable.getKeyTargetColumns().size();
                                count = execute(transaction, modelTable.getInsertStatement(),
                                        new Object(), data.subList(0, endIndex));
                            }
                        } else {
                            try {
                                int count = execute(transaction, modelTable.getInsertStatement(), new Object(),
                                        data);
                                componentStatistics.incrementNumberEntitiesProcessed(count);
                            } catch (UniqueKeyException e) {
                                if (replaceRows) {
                                    log.debug("Falling back to update");
                                    for (TargetColumn modelColumn : modelTable
                                            .getKeyTargetColumns()) {
                                        data.add(inputRow.get(modelColumn.getModelAttribute()
                                                .getId()));
                                    }
                                    execute(transaction, modelTable.getUpdateStatement(),
                                            new Object(), data);
                                } else {
                                    throw e;
                                }
                            }
                        }
                    }
                }
            }
            transaction.commit();
        } catch (Throwable ex) {
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

    private int execute(ISqlTransaction transaction, DmlStatement dmlStatement, Object marker,
            List<Object> data) {
        if (log.isDebugEnabled()) {
            log.debug("Preparing dml: " + dmlStatement.getSql());
        }
        transaction.prepare(dmlStatement.getSql());

        if (log.isDebugEnabled()) {
            log.debug("Submitting data {} with types {}", Arrays.toString(data.toArray()),
                    Arrays.toString(dmlStatement.getTypes()));
        }
        return transaction.addRow(marker, data.toArray(), dmlStatement.getTypes());
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

    class TargetTable {
        ModelEntity modelEntity;

        Table table;

        DmlStatement insertStatement;

        DmlStatement updateStatement;

        List<TargetColumn> keyTargetColumns = new ArrayList<TargetColumn>();

        List<TargetColumn> targetColumns = new ArrayList<TargetColumn>();

        public TargetTable(ModelEntity modelEntity, Table table, DmlStatement insertStatement,
                DmlStatement updateStatement) {
            this.modelEntity = modelEntity;
            this.table = table;
            this.insertStatement = insertStatement;
            this.updateStatement = updateStatement;
        }

        public ModelEntity getModelEntity() {
            return modelEntity;
        }

        public Table getTable() {
            return table;
        }

        public DmlStatement getInsertStatement() {
            return insertStatement;
        }

        public DmlStatement getUpdateStatement() {
            return updateStatement;
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

        TargetColumn(ModelAttribute modelAttribute, Column column) {
            this.modelAttribute = modelAttribute;
            this.column = column;
        }

        public ModelAttribute getModelAttribute() {
            return modelAttribute;
        }

        public Column getColumn() {
            return column;
        }
    }
}
