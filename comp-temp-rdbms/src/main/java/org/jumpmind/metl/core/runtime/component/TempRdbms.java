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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

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
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class TempRdbms extends AbstractRdbmsComponentRuntime  {

    public static String IN_MEMORY_DB = "in.memory.db";
    
    public static String CONTINUE_ON_ERROR = "continue.on.error";
    
    public static String BATCH_MODE = "batch.mode";
    
    public static String DDL = "ddl";

    int rowsPerMessage = 1000;

    boolean inMemoryDb = true;
    
    String runWhen = PER_UNIT_OF_WORK;

    IDatabasePlatform databasePlatform;

    RdbmsWriter databaseWriter;

    String databaseName;
    
    List<String> sqls;
    
    List<String> ddls;
    
    int rowReadDuringHandle;
    
    boolean continueOnError = false;
    
    boolean batchMode = false;

    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        sqls = getSqlStatements(true);
        ddls = getDdlStatements(false);

        this.inMemoryDb = properties.is(IN_MEMORY_DB);
        this.rowsPerMessage = properties.getInt(ROWS_PER_MESSAGE);
        this.continueOnError = properties.is(CONTINUE_ON_ERROR);
        this.batchMode = properties.is(BATCH_MODE);
        Component comp = context.getFlowStep().getComponent();
        Model inputModel = context.getFlowStep().getComponent().getInputModel();
        Model outputModel = context.getFlowStep().getComponent().getOutputModel();
        comp.setOutputModel(outputModel);
        if (inputModel == null) {
            throw new MisconfiguredException("The input model is not set and it is required");
        }
        runWhen = getComponent().get(RUN_WHEN, PER_UNIT_OF_WORK);
    }

    
    @Override
    public void handle(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        createDatabase();
        loadIntoDatabase(inputMessage);
        if ( (!PER_UNIT_OF_WORK.equals(runWhen) && !(inputMessage instanceof ControlMessage))
                || (PER_UNIT_OF_WORK.equals(runWhen) && unitOfWorkBoundaryReached)) {
            query(callback);
        }
    }

    protected void query(ISendMessageCallback callback) {
        RdbmsReader reader = new RdbmsReader();
        reader.setDataSource(databasePlatform.getDataSource());
        reader.setContext(context);
        reader.setComponentDefinition(componentDefinition);
        reader.setRowsPerMessage(rowsPerMessage);
        reader.setThreadNumber(threadNumber);
        
        reader.setSqls(sqls);
        reader.handle(new ControlMessage(this.context.getFlowStep().getId()), callback, false);
        info("Sent %d records", reader.getRowReadDuringHandle());

        ResettableBasicDataSource ds = databasePlatform.getDataSource();
        ds.close();

        if (!inMemoryDb) {
            try {
                Files.list(Paths.get(System.getProperty("h2.baseDir"))).filter(path -> path.toFile().getName().startsWith(databaseName))
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

    
    protected void loadIntoDatabase(Message message) {
        if (databaseWriter == null) {
            databaseWriter = new RdbmsWriter();
            databaseWriter.setDatabasePlatform(databasePlatform);
            databaseWriter.setComponentDefinition(componentDefinition);
            databaseWriter.setReplaceRows(true);
            databaseWriter.setContext(context);
            databaseWriter.setThreadNumber(threadNumber);
            databaseWriter.setContinueOnError(continueOnError);
            databaseWriter.setBatchMode(batchMode);
        }
        databaseWriter.handle(message, null, false);
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
            if (inMemoryDb) {
                ds.setUrl("jdbc:h2:mem:" + databaseName);
            } else {
                ds.setUrl("jdbc:h2:file:./" + databaseName);
            }
            databasePlatform = JdbcDatabasePlatformFactory.createNewPlatformInstance(ds, new SqlTemplateSettings(), true, false);

            Model inputModel = context.getFlowStep().getComponent().getInputModel();
            List<ModelEntity> entities = inputModel.getModelEntities();
            for (ModelEntity entity : entities) {
                Table table = new Table();
                table.setName(entity.getName());
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
                log(LogLevel.INFO, "Creating table: " + table.getName() + "  on db: " + databasePlatform.getDataSource().toString());
                
                // H2 database platform sets names to upper case by default unless 
                // the database name is mixed case. For our purposes, we 
                // always want the name to be case insensitive in the logical model.
                alterCaseToMatchLogicalCase(table);
                
                databasePlatform.createTables(false, false, table);
            }
            
            runDdls();

            log(LogLevel.INFO, "Creating databasePlatform with the following url: %s", ds.getUrl());
        }
    }
    
    protected void runDdls() {
    	if(ddls != null) {
    		JdbcOperations jdbcTemplate = getJdbcTemplate().getJdbcOperations();
    		for(String ddl : ddls) {
    			log(LogLevel.INFO, "Executing ddl %s", ddl);
    			jdbcTemplate.execute(ddl);
    		}
    	}
    }
    
    @Override
    protected NamedParameterJdbcTemplate getJdbcTemplate() {
    	DataSource dataSource = databasePlatform.getDataSource();
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setQueryTimeout(queryTimeout);
        return new NamedParameterJdbcTemplate(template);
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

    @Override
    public boolean supportsStartupMessages() {
        return false;
    }

}
