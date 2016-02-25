/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jumpmind.metl.core.runtime.component;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityData.ChangeType;
import org.jumpmind.metl.core.runtime.EntityDataMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.metl.core.util.LogUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;

public class RdbmsWriter extends AbstractRdbmsComponentRuntime {

    public static final String TYPE = "RDBMS Writer";
    public final static String CATALOG = "catalog";
    public final static String SCHEMA = "schema";
    public final static String REPLACE = "replace";
    public final static String UPDATE_FIRST = "update.first";
    public final static String INSERT_FALLBACK = "insert.fallback";
    public final static String QUOTE_IDENTIFIERS = "quote.identifiers";
    public final static String FIT_TO_COLUMN = "fit.to.column";
    public final static String ATTRIBUTE_INSERT_ENABLED = "insert.enabled";
    public final static String ATTRIBUTE_UPDATE_ENABLED = "update.enabled";
    public final static String BATCH_MODE = "batch.mode";
    public final static String CONTINUE_ON_ERROR = "continue.on.error";
    public final static String TABLE_SUFFIX = "table.suffix";
    public final static String TABLE_PREFIX = "table.prefix";

    boolean continueOnError = false;
    boolean replaceRows = false;
    boolean updateFirst = false;
    boolean insertFallback = false;
    boolean quoteIdentifiers = false;
    boolean fitToColumn = false;
    String catalogName;
    String schemaName;
    String tableSuffix = "";
    String tablePrefix = "";
    int inboundEntityDataCount = 0;
    int totalStatementCount = 0;
    boolean batchMode = false;
    IDatabasePlatform databasePlatform;
    List<TargetTableDefintion> targetTables;
    Throwable error;
    String lastPreparedDml;

    @Override
    protected void start() {

        inboundEntityDataCount = 0;
        error = null;

        if (getResourceRuntime() == null) {
            throw new IllegalStateException("A databasePlatform writer must have a datasource defined");
        }
        if (getInputModel() == null) {
            throw new IllegalStateException("A databasePlatform writer must have an input model defined");
        }

        TypedProperties properties = getTypedProperties();
        batchMode = properties.is(BATCH_MODE, batchMode);
        replaceRows = properties.is(REPLACE);
        continueOnError = properties.is(CONTINUE_ON_ERROR, continueOnError);
        updateFirst = properties.is(UPDATE_FIRST);
        insertFallback = properties.is(INSERT_FALLBACK);
        quoteIdentifiers = properties.is(QUOTE_IDENTIFIERS);
        fitToColumn = properties.is(FIT_TO_COLUMN);
        tableSuffix = properties.get(TABLE_SUFFIX, "");

        if (tableSuffix == null) {
            tableSuffix = "";
        }
        tablePrefix = properties.get(TABLE_PREFIX, "");
        if (tablePrefix == null) {
            tablePrefix = "";
        }
        catalogName = FormatUtils.replaceTokens(properties.get(CATALOG), context.getFlowParameters(), true);
        if (isBlank(catalogName)) {
            catalogName = null;
        }
        schemaName = FormatUtils.replaceTokens(properties.get(SCHEMA), context.getFlowParameters(), true);
        if (isBlank(schemaName)) {
            schemaName = null;
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    @Override
    public void handle(final Message inputMessage, final ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        if (inputMessage instanceof EntityDataMessage) {
            results.clear();
            lastPreparedDml = null;

            if (error == null) {
                if (databasePlatform == null) {
                    if (getResourceRuntime() == null) {
                        throw new RuntimeException("The data source resource has not been configured.  Please configure it.");
                    }
                    DataSource dataSource = (DataSource) getResourceReference();
                    databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource, new SqlTemplateSettings(),
                            quoteIdentifiers, false);
                }
                if (targetTables == null) {
                    Model model = getInputModel();
                    targetTables = new ArrayList<TargetTableDefintion>();
                    for (ModelEntity entity : model.getModelEntities()) {
                        String tableName = tablePrefix + entity.getName() + tableSuffix;
                        Table table = databasePlatform.getTableFromCache(catalogName, schemaName, tableName, true);
                        if (table != null) {
                            targetTables.add(new TargetTableDefintion(entity, new TargetTable(DmlType.UPDATE, entity, table.copy()),
                                    new TargetTable(DmlType.INSERT, entity, table.copy()),
                                    new TargetTable(DmlType.DELETE, entity, table.copy())));
                        }
                    }
                }

                ArrayList<EntityData> inputRows = ((EntityDataMessage) inputMessage).getPayload();
                if (inputRows != null && inputRows.size() > 0) {
                    ISqlTransaction transaction = databasePlatform.getSqlTemplate().startSqlTransaction();
                    transaction.setInBatchMode(batchMode);
                    try {
                        write(transaction, (EntityDataMessage)inputMessage, callback, unitOfWorkBoundaryReached);
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
                if (callback != null && results.size() > 0) {
                    callback.sendTextMessage(null, convertResultsToTextPayload(results));
                }
            }

            if (targetTables != null) {
                for (TargetTableDefintion targetTable : targetTables) {
                    targetTable.getDeleteTable().getRowValues().clear();
                    targetTable.getInsertTable().getRowValues().clear();
                    targetTable.getUpdateTable().getRowValues().clear();
                }
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

        return modelTable.getStatement().getValueArray(data.toArray(new Object[data.size()]),
                keyValues.toArray(new Object[keyValues.size()]));
    }

    private void sortAndStoreRowsByTableAndOperation(List<EntityData> inputRows) {
        TargetTable modelTable = null;
        boolean processedRow = false;
        int order = 0;
        for (EntityData inputRow : inputRows) {
            for (TargetTableDefintion targetTableDefinition : targetTables) {
                if (inputRow.getChangeType() == ChangeType.DEL) {
                    modelTable = targetTableDefinition.getDeleteTable();
                } else if (updateFirst || inputRow.getChangeType() == ChangeType.CHG) {
                    modelTable = targetTableDefinition.getUpdateTable();
                } else if (inputRow.getChangeType() == ChangeType.ADD) {
                    modelTable = targetTableDefinition.getInsertTable();
                }
                if (modelTable.shouldProcess(inputRow)) {
                    processedRow = true;
                    modelTable.getRowValues().add(inputRow);
                    if (targetTableDefinition.getOrder() == null) {
                        targetTableDefinition.setOrder(order++);
                    }
                }
            } // end each target table option
            
            String entityNameToBeProcessed="Not Found";
            if (!processedRow) {
                Model inputModel = getInputModel();
                for (ModelEntity entity : inputModel.getModelEntities()) {
                    for (ModelAttribute attribute : entity.getModelAttributes()) {
                        if (inputRow.containsKey(attribute.getId())) {
                            entityNameToBeProcessed = entity.getName();
                        }
                    }
                }
                throw new MisconfiguredException("Could not find table to write to for row.  Operation %s. " +
                "Entity %s. Row values %s", inputRow.getChangeType(), entityNameToBeProcessed, inputRow.toString());
            }
            processedRow = false;
        } // end for each row

        Collections.sort(targetTables);
    }

    private void executeSqlByTableAndOperation(ISqlTransaction transaction, Map<TargetTableDefintion, WriteStats> statsMap) {
        for (TargetTableDefintion targetTableDefinition : targetTables) {
            WriteStats stats = getStats(targetTableDefinition, statsMap);
            executeSqlDeletes(targetTableDefinition.getDeleteTable(), transaction, stats);
            executeSqlChanges(targetTableDefinition, transaction, stats);
            executeSqlInserts(targetTableDefinition, transaction, stats);
        }
    }

    private WriteStats getStats(TargetTableDefintion targetTableDefinition, Map<TargetTableDefintion, WriteStats> statsMap) {

        WriteStats stats = statsMap.get(targetTableDefinition);
        if (stats == null) {
            stats = new WriteStats();
            statsMap.put(targetTableDefinition, stats);
        }
        return stats;
    }

    private void executeSqlDeletes(TargetTable targetTable, ISqlTransaction transaction, WriteStats stats) {
        for (EntityData inputRow : targetTable.getRowValues()) {
            Object[] rowData = getValues(false, targetTable, inputRow);
            int count = executeSql(targetTable, transaction, rowData);
            stats.deleteCount += count;
        }
    }

    private void executeSqlChanges(TargetTableDefintion targetTableDefinition, ISqlTransaction transaction, WriteStats stats) {

        TargetTable targetUpdateTable = targetTableDefinition.getUpdateTable();
        TargetTable targetInsertTable = targetTableDefinition.getInsertTable();

        for (EntityData inputRow : targetUpdateTable.getRowValues()) {
            Object[] rowData = getValues(false, targetUpdateTable, inputRow);
            int count = executeSql(targetUpdateTable, transaction, rowData);
            stats.updateCount += count;
            if (insertFallback && count == 0) {
                log.debug("Falling back to insert");
                rowData = getValues(false, targetInsertTable, inputRow);
                count = executeSql(targetInsertTable, transaction, rowData);
                stats.fallbackInsertCount += count;
            } else if (count == 0 && !continueOnError) {
                throw new SqlException(String.format("Failed to update row: \n%s\nWith values: \n%s\nWith types: \n%s\n",
                        targetUpdateTable.getStatement().getSql(), Arrays.toString(rowData),
                        Arrays.toString(targetUpdateTable.getStatement().getTypes())));
            } else if (count == 0) {
                stats.ignoredCount++;
            }
        }
    }

    private void executeSqlInserts(TargetTableDefintion targetTableDefinition, ISqlTransaction transaction, WriteStats stats) {

        TargetTable targetUpdateTable = targetTableDefinition.getUpdateTable();
        TargetTable targetInsertTable = targetTableDefinition.getInsertTable();

        for (EntityData inputRow : targetInsertTable.getRowValues()) {
            try {
                Object[] rowData = getValues(false, targetInsertTable, inputRow);
                int count = executeSql(targetInsertTable, transaction, rowData);
                stats.insertCount += count;
            } catch (UniqueKeyException e) {
                if (replaceRows) {
                    log.debug("Falling back to update");
                    Object[] rowData = getValues(false, targetUpdateTable, inputRow);
                    int count = execute(transaction, targetUpdateTable.getStatement(), new Object(), rowData);
                    stats.fallbackUpdateCount += count;
                } else if (!continueOnError) {
                    throw e;
                } else {
                    stats.ignoredCount++;
                }
            }
        }
    }

    private int executeSql(TargetTable targetTable, ISqlTransaction transaction, Object[] rowData) {

        int count = execute(transaction, targetTable.getStatement(), new Object(), rowData);
        if (count > 0) {
            results.add(new Result(targetTable.getStatement().getSql(), count));
            totalStatementCount++;
            getComponentStatistics().incrementNumberEntitiesProcessed(count);
        }
        return count;
    }

    private void write(ISqlTransaction transaction, EntityDataMessage inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {

        long ts = System.currentTimeMillis();
        Map<TargetTableDefintion, WriteStats> statsMap = new HashMap<TargetTableDefintion, WriteStats>();
        sortAndStoreRowsByTableAndOperation(inputMessage.getPayload());
        executeSqlByTableAndOperation(transaction, statsMap);
        int totalStatementCount = writeStats(statsMap);       
        info("Ran a total of %d statements in %s", totalStatementCount, LogUtils.formatDuration(System.currentTimeMillis() - ts));
    }

    private int writeStats(Map<TargetTableDefintion, WriteStats> statsMap) {
        int rowCount = 0;
        for (TargetTableDefintion table : targetTables) {
            WriteStats stats = statsMap.get(table);
            if (stats != null) {
                StringBuilder msg = new StringBuilder();
                if (stats.insertCount > 0) {
                    msg.append("Inserted: ");
                    msg.append(stats.insertCount);
                    rowCount += stats.insertCount;
                }
                if (stats.fallbackUpdateCount > 0) {
                    if (msg.length() > 0) {
                        msg.append(", ");
                    }
                    msg.append("Fallback Updates: ");
                    msg.append(stats.fallbackUpdateCount);
                    rowCount += stats.fallbackUpdateCount * 2;
                }
                if (stats.updateCount > 0) {
                    if (msg.length() > 0) {
                        msg.append(", ");
                    }
                    msg.append("Updated: ");
                    msg.append(stats.updateCount);
                    rowCount += stats.updateCount;
                }
                if (stats.deleteCount > 0) {
                    if (msg.length() > 0) {
                        msg.append(", ");
                    }
                    msg.append("Deleted: ");
                    msg.append(stats.deleteCount);
                    rowCount += stats.deleteCount;
                }
                if (stats.fallbackInsertCount > 0) {
                    if (msg.length() > 0) {
                        msg.append(", ");
                    }
                    msg.append("Fallback Inserts: ");
                    msg.append(stats.fallbackInsertCount);
                    rowCount += stats.fallbackInsertCount * 2;
                }
                if (stats.ignoredCount > 0) {
                    if (msg.length() > 0) {
                        msg.append(", ");
                    }
                    msg.append("Ignored Count: ");
                    msg.append(stats.ignoredCount);
                    rowCount += stats.ignoredCount;
                }
                if (msg.length() > 0) {
                    log(LogLevel.INFO, "%s: %s", table.getInsertTable().getTable().getFullyQualifiedTableName(), msg.toString());
                }
            }
        }
        return rowCount;
    }

    private int execute(ISqlTransaction transaction, DmlStatement dmlStatement, Object marker, Object[] data) {

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
            if (!(replaceRows && ex instanceof UniqueKeyException)) {
                if (continueOnError) {
                    log(LogLevel.WARN, String.format("Failed to run the following sql: \n%s\nWith values: \n%s\nWith types: \n%s\n."
                            + "Continue on Error flag set - Continuing load",
                            dmlStatement.getSql(), Arrays.toString(data), Arrays.toString(dmlStatement.getTypes())));
                    return 0;
                } else {
                    log(LogLevel.ERROR, String.format("Failed to run the following sql: \n%s\nWith values: \n%s\nWith types: \n%s\n",
                            dmlStatement.getSql(), Arrays.toString(data), Arrays.toString(dmlStatement.getTypes())));
                    throw ex;
                }
            } else {
                throw ex;
            }
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

    public void setTablePrefix(String tablePrefix) {
        this.tablePrefix = tablePrefix;
        this.targetTables = null;
    }

    public void setTableSuffix(String tableSuffix) {
        this.tableSuffix = tableSuffix;
        this.targetTables = null;
    }

    public void setBatchMode(boolean batchMode) {
        this.batchMode = batchMode;
    }

    public void setFitToColumn(boolean fitToColumn) {
        this.fitToColumn = fitToColumn;
    }

    public void setInsertFallback(boolean insertFallback) {
        this.insertFallback = insertFallback;
    }

    public void setCatalogName(String catalogName) {
        this.catalogName = catalogName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public void setDatabasePlatform(IDatabasePlatform platform) {
        this.databasePlatform = platform;
    }

    public void setReplaceRows(boolean replaceRows) {
        this.replaceRows = replaceRows;
    }

    public void setUpdateFirst(boolean updateFirst) {
        this.updateFirst = updateFirst;
    }
    
    public Throwable getError() {
        return error;
    }

    class TargetTableDefintion implements Comparable<TargetTableDefintion> {
        ModelEntity modelEntity;
        TargetTable updateTable;
        TargetTable insertTable;
        TargetTable deleteTable;
        Integer order;

        public TargetTableDefintion(ModelEntity modelEntity, TargetTable updateTable, TargetTable insertTable, TargetTable deleteTable) {

            super();
            this.modelEntity = modelEntity;
            this.updateTable = updateTable;
            this.insertTable = insertTable;
            this.deleteTable = deleteTable;
        }

        public TargetTable getDeleteTable() {
            return deleteTable;
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

        public Integer getOrder() {
            return order;
        }

        public void setOrder(Integer order) {
            this.order = order;
        }

        @Override
        public int compareTo(TargetTableDefintion o) {
            if (o.order == null && order != null) {
                return 1;
            } else if (order == null && o.order != null) {
                return -1;
            } else if (order == null && o.order == null) {
                return 0;
            } else {
                return order.compareTo(o.order);
            }
        }

    }

    class TargetTable {
        Table table;
        DmlStatement statement;
        List<TargetColumn> keyTargetColumns = new ArrayList<TargetColumn>();
        List<TargetColumn> targetColumns = new ArrayList<TargetColumn>();
        List<EntityData> rowValues = new ArrayList<EntityData>();

        public TargetTable(DmlType dmlType, ModelEntity entity, Table table) {
            this.table = table;
            List<ModelAttribute> attributes = entity.getModelAttributes();
            String[] columnNames = table.getColumnNames();
            /*
             * 
             * Remove columns that don't exist in the model
             * 
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
            if (dmlType == DmlType.INSERT || dmlType == DmlType.UPDATE) {
                /*
                 * 
                 * Remove columns that are not enabled for this dml type
                 * 
                 */
                for (ModelAttribute attribute : attributes) {
                    ComponentAttributeSetting setting = getComponent().getSingleAttributeSetting(attribute.getId(),
                            dmlType == DmlType.INSERT ? ATTRIBUTE_INSERT_ENABLED : ATTRIBUTE_UPDATE_ENABLED);
                    if (setting != null && !Boolean.parseBoolean(setting.getValue())) {
                        table.removeColumn(table.findColumn(attribute.getName()));
                    }
                }
            }
            statement = databasePlatform.createDmlStatement(dmlType, table, null);
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

        public List<EntityData> getRowValues() {
            return this.rowValues;
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
        int ignoredCount;
        int insertCount;
        int deleteCount;
        int updateCount;
        int fallbackInsertCount;
        int fallbackUpdateCount;
    }
}
