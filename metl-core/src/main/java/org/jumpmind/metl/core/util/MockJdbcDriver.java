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
package org.jumpmind.metl.core.util;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Properties;

import org.jumpmind.db.sql.LogSqlBuilder;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.runtime.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mockrunner.jdbc.CallableStatementResultSetHandler;
import com.mockrunner.jdbc.PreparedStatementResultSetHandler;
import com.mockrunner.jdbc.StatementResultSetHandler;
import com.mockrunner.mock.jdbc.MockConnection;
import com.mockrunner.mock.jdbc.MockDatabaseMetaData;
import com.mockrunner.mock.jdbc.MockParameterMap;
import com.mockrunner.mock.jdbc.MockResultSet;

public class MockJdbcDriver extends com.mockrunner.mock.jdbc.MockDriver {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected StatementResultSetHandler statementHandler;
    protected PreparedStatementResultSetHandler preparedStatementHandler;
    protected CallableStatementResultSetHandler callableStatementHandler;
    protected IConfigurationService configurationService;

    public MockJdbcDriver() {
    }

    public MockJdbcDriver(IConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:mock");
    }

    public CallableStatementResultSetHandler getCallableStatementHandler() {
        return callableStatementHandler;
    }

    public PreparedStatementResultSetHandler getPreparedStatementHandler() {
        return preparedStatementHandler;
    }

    public StatementResultSetHandler getStatementHandler() {
        return statementHandler;
    }
    
    protected Object[] toObjectArray(MockParameterMap parameters) {
        Object[] array = new Object[parameters.size()];
        for(int i = 1; i < parameters.size(); i++) {
            array[i-1] = parameters.get(i);
        }
        return array;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        statementHandler = new StatementResultSetHandler();
        preparedStatementHandler = new PreparedStatementResultSetHandler() {
            @Override
            public void addParameterMapForExecutedStatement(String sql, MockParameterMap parameters) {
                super.addParameterMapForExecutedStatement(sql, parameters);
                log.info(new LogSqlBuilder().buildDynamicSqlForLog(sql, toObjectArray(parameters), null) + ";");
            }
        };
        callableStatementHandler = new CallableStatementResultSetHandler();
        MockConnection connection = new MockConnection(statementHandler, preparedStatementHandler, callableStatementHandler);
        MockDatabaseMetaData mockMetaData = new MockDatabaseMetaData() {
            public java.sql.ResultSet getTables(String catalog, String schemaPattern, String tableNamePattern, String[] types)
                    throws SQLException {
                MockResultSet tables = new MockResultSet("tables");
                tables.addColumn("TABLE_NAME", new Object[] { tableNamePattern });
                return tables;
            };

            public java.sql.ResultSet getColumns(String catalog, String schemaPattern, String tableNamePattern, String columnNamePattern)
                    throws SQLException {
                MockResultSet columns = new MockResultSet("columns");
                columns.addColumn("TABLE_NAME");
                columns.addColumn("COLUMN_NAME");
                columns.addColumn("TYPE_NAME");
                columns.addColumn("DATA_TYPE");
                String projectVersionId = ComponentContext.projectVersionId.get();
                if (isNotBlank(projectVersionId)) {
                    List<ModelName> models = configurationService.findModelsInProject(projectVersionId);
                    for (ModelName modelName : models) {
                        Model model = configurationService.findModel(modelName.getId());
                        List<ModelEntity> entities = model.getModelEntities();
                        for (ModelEntity modelEntity : entities) {
                            if (modelEntity.getName().equals(tableNamePattern)) {
                                List<ModelAttrib> attributes = modelEntity.getModelAttributes();
                                for (ModelAttrib attribute : attributes) {
                                    DataType type = attribute.getDataType();
                                    int typeNumber = Types.VARCHAR;
                                    if (type.isBinary()) {
                                        typeNumber = Types.BLOB;
                                    } else if (type.isNumeric()) {
                                        typeNumber = Types.NUMERIC;
                                    } else if (type.isBoolean()) {
                                        typeNumber = Types.BOOLEAN;
                                    } else if (type.isTimestamp()) {
                                        typeNumber = Types.TIMESTAMP;
                                    }
                                    columns.addRow(new Object[] { tableNamePattern, attribute.getName(), type.name(), typeNumber });
                                }
                                break;
                            }
                        }
                    }

                }
                return columns;
            };

            @Override
            public ResultSet getPrimaryKeys(String catalog, String schema, String table) throws SQLException {
                MockResultSet columns = new MockResultSet("columns");
                columns.addColumn("TABLE_NAME");
                columns.addColumn("COLUMN_NAME");
                columns.addColumn("PK_NAME");
                String projectVersionId = ComponentContext.projectVersionId.get();
                if (isNotBlank(projectVersionId)) {
                    List<ModelName> models = configurationService.findModelsInProject(projectVersionId);
                    for (ModelName modelName : models) {
                        Model model = configurationService.findModel(modelName.getId());
                        List<ModelEntity> entities = model.getModelEntities();
                        for (ModelEntity modelEntity : entities) {
                            if (modelEntity.getName().equals(table)) {
                                List<ModelAttrib> attributes = modelEntity.getModelAttributes();
                                for (ModelAttrib attribute : attributes) {
                                    columns.addRow(new Object[] { table, attribute.getName(), attribute.isPk() });
                                }
                                break;
                            }
                        }
                    }

                }
                return columns;
            }
        };

        mockMetaData.setIndexInfo(new MockResultSet(""));
        mockMetaData.setTypeInfo(new MockResultSet(""));
        connection.setMetaData(mockMetaData);
        setupConnection(connection);
        return super.connect(url, info);
    }

}
