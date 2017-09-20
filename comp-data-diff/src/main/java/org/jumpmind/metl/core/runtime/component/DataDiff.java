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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.h2.Driver;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.IIndex;
import org.jumpmind.db.model.IndexColumn;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.ResettableBasicDataSource;
import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.ComponentAttribSetting;
import org.jumpmind.metl.core.model.ComponentEntitySetting;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData.ChangeType;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;

public class DataDiff extends AbstractComponentRuntime {

    public static String SOURCE_1 = "source.1";
    public static String SOURCE_2 = "source.2";
    public static String IN_MEMORY_COMPARE = "in.memory.compare";

    public final static String ENTITY_ADD_ENABLED = "add.enabled";

    public final static String ENTITY_CHG_ENABLED = "chg.enabled";

    public final static String ENTITY_DEL_ENABLED = "del.enabled";

    public final static String ENTITY_ORDER = "order";

    public final static String ATTRIBUTE_COMPARE_ENABLED = "compare.enabled";

    int rowsPerMessage = 1000;

    String sourceStep1Id;

    String sourceStep2Id;

    boolean inMemoryCompare = true;

    IDatabasePlatform databasePlatform;

    RdbmsWriter databaseWriter;

    String databaseName;

    List<ModelEntity> entities;

    Throwable error;

    @Override
    public void start() {
        error = null;
        TypedProperties properties = getTypedProperties();
        this.sourceStep1Id = properties.get(SOURCE_1);
        if (isBlank(sourceStep1Id)) {
            throw new MisconfiguredException(
                    "Please choose a step where the original data comes from");
        }
        this.sourceStep2Id = properties.get(SOURCE_2);
        if (isBlank(sourceStep2Id)) {
            throw new MisconfiguredException(
                    "Please choose a step where the data to compare comes from");
        }

        this.inMemoryCompare = properties.is(IN_MEMORY_COMPARE);
        this.rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);
        Component comp = context.getFlowStep().getComponent();
        comp.setOutputModel(comp.getInputModel());
        Model inputModel = context.getFlowStep().getComponent().getInputModel();
        if (inputModel == null) {
            throw new MisconfiguredException("The input model is not set and it is required");
        }

        entities = new ArrayList<>(inputModel.getModelEntities());
        Collections.sort(entities, new Comparator<ModelEntity>() {
            @Override
            public int compare(ModelEntity o1, ModelEntity o2) {
                ComponentEntitySetting order1 = context.getFlowStep().getComponent()
                        .getSingleEntitySetting(o1.getId(), DataDiff.ENTITY_ORDER);
                int orderValue1 = order1 != null ? Integer.parseInt(order1.getValue()) : 0;

                ComponentEntitySetting order2 = context.getFlowStep().getComponent()
                        .getSingleEntitySetting(o2.getId(), DataDiff.ENTITY_ORDER);
                int orderValue2 = order2 != null ? Integer.parseInt(order2.getValue()) : 0;

                return new Integer(orderValue1).compareTo(new Integer(orderValue2));
            }
        });
    }

    @Override
    public void handle(Message message, ISendMessageCallback callback,
            boolean unitOfWorkBoundaryReached) {
        createDatabase();
        loadIntoDatabase(message);
        if (unitOfWorkBoundaryReached && error == null) {
            calculateDiff(callback);
        }
    }
    
    /*
     * Column names must be quoted in order to handle names with spaces.
     * Note: Quoting the name also makes the name case sensitive in H2.
     * Make the name upper case to match the table create statement.
     */
    protected String getFormattedName(AbstractNamedObject o) {
        return "\"" + o.getName().toUpperCase() + "\"";
    }

    protected void calculateDiff(ISendMessageCallback callback) {
        Map<ModelEntity, String> changeSqls = new HashMap<>();
        Map<ModelEntity, String> addSqls = new HashMap<>();
        Map<ModelEntity, String> delSqls = new HashMap<>();
        Component component = context.getFlowStep().getComponent();
        for (ModelEntity entity : entities) {
            StringBuilder addSql = new StringBuilder("select ");
            StringBuilder chgSql = new StringBuilder(addSql);
            StringBuilder delSql = new StringBuilder(addSql);
            appendColumns(addSql, "curr.", entity);
            appendColumns(delSql, "orig.", entity);
            appendColumns(chgSql, "curr.", entity);

            addSql.append(" from " + entity.getName() + "_2 curr left join " + entity.getName()
                    + "_1 orig on ");
            delSql.append(" from " + entity.getName() + "_1 orig left join " + entity.getName()
                    + "_2 curr on ");
            chgSql.append(" from " + entity.getName() + "_1 orig join " + entity.getName()
                    + "_2 curr on ");
            boolean secondPk = false;
            for (ModelAttrib attribute : entity.getModelAttributes()) {
                if (attribute.isPk()) {
                    if (secondPk) {
                        addSql.append(" and ");
                        delSql.append(" and ");
                        chgSql.append(" and ");
                    }
                    addSql.append("curr.").append(getFormattedName(attribute)).append("=").append("orig.")
                            .append(getFormattedName(attribute));
                    delSql.append("curr.").append(getFormattedName(attribute)).append("=").append("orig.")
                            .append(getFormattedName(attribute));
                    chgSql.append("curr.").append(getFormattedName(attribute)).append("=").append("orig.")
                            .append(getFormattedName(attribute));
                    secondPk = true;
                }
            }

            addSql.append(" where ");
            delSql.append(" where ");
            chgSql.append(" where ");
            secondPk = false;
            boolean secondCol = false;
            for (ModelAttrib attribute : entity.getModelAttributes()) {
                if (attribute.isPk()) {
                    if (secondPk) {
                        addSql.append(" or ");
                        delSql.append(" or ");
                    }
                    addSql.append("orig.").append(getFormattedName(attribute)).append(" is null");
                    delSql.append("curr.").append(getFormattedName(attribute)).append(" is null");
                    secondPk = true;
                } else {
                    ComponentAttribSetting matchColumnSetting = component
                            .getSingleAttributeSetting(attribute.getId(),
                                    DataDiff.ATTRIBUTE_COMPARE_ENABLED);
                    boolean matchColumn = matchColumnSetting != null
                            ? Boolean.parseBoolean(matchColumnSetting.getValue()) : true;
                    if (matchColumn) {
                        if (secondCol) {
                            chgSql.append(" or ");
                        }
                        chgSql.append("curr.").append(getFormattedName(attribute)).append(" != ")
                                .append("orig.").append(getFormattedName(attribute));
                        chgSql.append(" or ");
                        chgSql.append("curr.").append(getFormattedName(attribute)).append(" is null and ")
                                .append("orig.").append(getFormattedName(attribute))
                                .append(" is not null ");
                        chgSql.append(" or ");
                        chgSql.append("curr.").append(getFormattedName(attribute))
                                .append(" is not null and ").append("orig.")
                                .append(getFormattedName(attribute)).append(" is null ");
                        secondCol = true;
                    }
                }
            }

            //we only want to do a change compare if this entity has 
            //cols to compare other than the primary key.
            if (!entity.hasOnlyPrimaryKeys() && secondCol) {
                changeSqls.put(entity, chgSql.toString());
                log(LogLevel.INFO, "Generated diff sql for CHG: %s", chgSql);
            }
            log(LogLevel.INFO, "Generated diff sql for ADD: %s", addSql);
            log(LogLevel.INFO, "Generated diff sql for DEL: %s", delSql);
            addSqls.put(entity, addSql.toString());
            delSqls.put(entity, delSql.toString());

        }

        RdbmsReader reader = new RdbmsReader();
        reader.setDataSource(databasePlatform.getDataSource());
        reader.setContext(context);
        reader.setComponentDefinition(componentDefinition);
        reader.setRowsPerMessage(rowsPerMessage);
        reader.setThreadNumber(threadNumber);

        for (ModelEntity entity : entities) {
            ComponentEntitySetting add = component.getSingleEntitySetting(entity.getId(),
                    DataDiff.ENTITY_ADD_ENABLED);
            ComponentEntitySetting chg = component.getSingleEntitySetting(entity.getId(),
                    DataDiff.ENTITY_CHG_ENABLED);
            boolean addEnabled = add != null ? Boolean.parseBoolean(add.getValue()) : true;
            boolean chgEnabled = chg != null ? Boolean.parseBoolean(chg.getValue()) : true;
            if (addEnabled) {
                reader.setSql(addSqls.get(entity));
                reader.setEntityChangeType(ChangeType.ADD);
                reader.handle(new ControlMessage(this.context.getFlowStep().getId()), callback,
                        false);
                info("Sent %d ADD records for %s", reader.getRowReadDuringHandle(),
                        entity.getName());
            }

            if (chgEnabled && changeSqls.get(entity) != null) {
                reader.setSql(changeSqls.get(entity));
                reader.setEntityChangeType(ChangeType.CHG);
                reader.handle(new ControlMessage(this.context.getFlowStep().getId()), callback,
                        false);
                info("Sent %d CHG records for %s", reader.getRowReadDuringHandle(),
                        entity.getName());
            }

        }

        for (int i = entities.size() - 1; i >= 0; i--) {
            ModelEntity entity = entities.get(i);
            ComponentEntitySetting del = component.getSingleEntitySetting(entity.getId(),
                    DataDiff.ENTITY_DEL_ENABLED);
            boolean delEnabled = del != null ? Boolean.parseBoolean(del.getValue()) : true;

            if (delEnabled) {
                reader.setSql(delSqls.get(entity));
                reader.setEntityChangeType(ChangeType.DEL);
                reader.handle(new ControlMessage(this.context.getFlowStep().getId()), callback,
                        false);
                info("Sent %d DEL records for %s", reader.getRowReadDuringHandle(),
                        entity.getName());
            }
        }

        ResettableBasicDataSource ds = databasePlatform.getDataSource();

        ds.close();

        if (!inMemoryCompare) {
            try {
                Files.list(Paths.get(System.getProperty("h2.baseDir")))
                        .filter(path -> path.toFile().getName().startsWith(databaseName))
                        .forEach(path -> deleteDatabaseFile(path.toFile()));
            } catch (IOException e) {
                log.warn("Failed to delete file", e);
            }
        }

        databasePlatform = null;
        databaseName = null;
        databaseWriter = null;

    }

    protected void deleteDatabaseFile(File file) {
        log(LogLevel.INFO, "Deleting database file: %s", file.getName());
        FileUtils.deleteQuietly(file);
    }

    protected void appendColumns(StringBuilder sql, String prefix, ModelEntity entity) {
        for (ModelAttrib attribute : entity.getModelAttributes()) {

            Component component = context.getFlowStep().getComponent();
            ComponentAttribSetting matchColumnSetting = component.getSingleAttributeSetting(
                    attribute.getId(), DataDiff.ATTRIBUTE_COMPARE_ENABLED);
            boolean matchColumn = matchColumnSetting != null
                    ? Boolean.parseBoolean(matchColumnSetting.getValue()) : true;
            if (matchColumn) {
                sql.append(prefix).append(getFormattedName(attribute)).append(" /* ")
                        .append(entity.getName()).append(".").append(attribute.getName())
                        .append(" */").append(",");
            }
        }
        sql.replace(sql.length() - 1, sql.length(), "");
    }

    protected void loadIntoDatabase(Message message) {
        String originatingStepId = message.getHeader().getOriginatingStepId();
        String tableSuffix = null;
        if (sourceStep1Id.equals(originatingStepId)) {
            tableSuffix = "_1";
        } else if (sourceStep2Id.equals(originatingStepId)) {
            tableSuffix = "_2";
        }

        if (databaseWriter == null) {
            databaseWriter = new RdbmsWriter();
            databaseWriter.setDatabasePlatform(databasePlatform);
            databaseWriter.setComponentDefinition(componentDefinition);
            databaseWriter.setReplaceRows(true);
            databaseWriter.setContext(context);
            databaseWriter.setThreadNumber(threadNumber);
        }

        if (tableSuffix != null) {
            databaseWriter.setTableSuffix(tableSuffix);
            try {
                databaseWriter.handle(message, null, false);
            } finally {
                error = databaseWriter.getError();
            }
        }
    }

    protected void createDatabase() {
        if (databasePlatform == null) {
            ResettableBasicDataSource ds = new ResettableBasicDataSource();
            ds.setDriverClassName(Driver.class.getName());
            ds.setMaxActive(1);
            ds.setInitialSize(1);
            ds.setMinIdle(1);
            ds.setMaxIdle(1);
            databaseName = UUID.randomUUID().toString();
            if (inMemoryCompare) {
                ds.setUrl("jdbc:h2:mem:" + databaseName);
            } else {
                ds.setUrl("jdbc:h2:file:./" + databaseName);
            }
            databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(ds,
                    new SqlTemplateSettings(), true, false);

            Model inputModel = context.getFlowStep().getComponent().getInputModel();
            List<ModelEntity> entities = inputModel.getModelEntities();
            for (ModelEntity entity : entities) {
                Table table = new Table();
                table.setName(entity.getName() + "_1");
                List<ModelAttrib> attributes = entity.getModelAttributes();
                for (ModelAttrib attribute : attributes) {
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

                    column.setPrimaryKey(attribute.isPk());
                    table.addColumn(column);
                }
                alterCaseToMatchLogicalCase(table);
                databasePlatform.createTables(false, false, table);

                table.setName(entity.getName().toUpperCase() + "_2");
                databasePlatform.createTables(false, false, table);

            }

            log(LogLevel.INFO, "Creating databasePlatform with the following url: %s", ds.getUrl());
        }
    }

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

    private void alterCaseToMatchLogicalCase(Table table) {
        table.setName(table.getName().toUpperCase());

        Column[] columns = table.getColumns();
        for (Column column : columns) {
            column.setName(column.getName().toUpperCase());
        }

        IIndex[] indexes = table.getIndices();
        for (IIndex index : indexes) {
            index.setName(index.getName().toUpperCase());

            IndexColumn[] indexColumns = index.getColumns();
            for (IndexColumn indexColumn : indexColumns) {
                indexColumn.setName(indexColumn.getName().toUpperCase());
            }
        }
    }

}
