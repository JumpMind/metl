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

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.db.sql.SqlException;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.util.FormatUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.StringUtils;

public class RdbmsReader extends AbstractRdbmsComponentRuntime {

    public static final String TYPE = "RDBMS Reader";

    public final static String ROWS_PER_MESSAGE = "rows.per.message";

    public final static String TRIM_COLUMNS = "trim.columns";

    public final static String MATCH_ON_COLUMN_NAME_ONLY = "match.on.column.name";

    List<String> sqls;

    long rowsPerMessage;

    boolean trimColumns = false;

    boolean matchOnColumnNameOnly = false;
    
    @Override
    protected void start() {
        TypedProperties properties = getTypedProperties();
        sqls = getSqlStatements();
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        trimColumns = properties.is(TRIM_COLUMNS);
        matchOnColumnNameOnly = properties.is(MATCH_ON_COLUMN_NAME_ONLY, false);
    }
        
    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(final Message inputMessage, final ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {
        NamedParameterJdbcTemplate template = getJdbcTemplate();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        int inboundRecordCount = 1;
        ArrayList<EntityData> inboundPayload = null;
        if (!(inputMessage instanceof StartupMessage)) {
            inboundPayload = inputMessage.getPayload();
            if (inboundPayload != null) {
                inboundRecordCount = inboundPayload.size();
            }
        }

        /*
         * A reader can be started by a startup message (if it has no input
         * links) or it can be started by another component that sends messages
         * to it. If the reader is started by another component, then loop for
         * all records in the input message
         */
        ArrayList<EntityData> outboundPayload = null;
        for (int i = 0; i < inboundRecordCount; i++) {
            if (inboundPayload != null && inboundPayload.size() > i && inboundPayload.get(i) instanceof EntityData) {
                setParamsFromInboundMsgAndRec(paramMap, inputMessage, inboundPayload.get(i));
            } else {
                setParamsFromInboundMsgAndRec(paramMap, inputMessage, null);
            }

            ResultSetToEntityDataConverter resultSetToEntityDataConverter = new ResultSetToEntityDataConverter(inputMessage, callback, unitOfWorkBoundaryReached);
            for (String sql : getSqls()) {
                String sqlToExecute = FormatUtils.replaceTokens(sql, getComponentContext().getFlowParametersAsString(), true);
                log(LogLevel.INFO, "About to run: " + sqlToExecute);
                resultSetToEntityDataConverter.setSqlToExecute(sqlToExecute);
                outboundPayload = template.query(sqlToExecute, paramMap, resultSetToEntityDataConverter);                        
            }
        }
        if (outboundPayload != null && outboundPayload.size() > 0) {
            callback.sendMessage(outboundPayload, true);
        }
    }

    private ArrayList<String> getAttributeIds(ResultSetMetaData meta, Map<Integer, String> sqlEntityHints) throws SQLException {

        ArrayList<String> attributeIds = new ArrayList<String>();

        for (int i = 1; i <= meta.getColumnCount(); i++) {
            String columnName = meta.getColumnName(i);
            String tableName = meta.getTableName(i);
            if (sqlEntityHints.containsKey(i)) {
                String hint = sqlEntityHints.get(i);
                if (hint.indexOf(".") != -1) {
                    tableName = hint.substring(0, hint.indexOf("."));
                    columnName = hint.substring(hint.indexOf(".") + 1);
                } else {
                    tableName = hint;
                }
            }

            if (matchOnColumnNameOnly) {
                attributeIds.addAll(getAttributeIds(columnName));
            } else {
                if (StringUtils.isEmpty(tableName)) {
                    throw new SQLException("Table name could not be determined from metadata or hints.  Please check column and hint.  "
                            + "(Note to SQL-Server users: metadata may not be returned unless you append 'FOR BROWSE' to the end of your query "
                            + "or set 'useCursors=true' on the JDBC URL.)"
                            + "Query column = " + i);
                }
                String attributeId = getAttributeId(tableName, columnName);
                attributeIds.add(attributeId);
            }
        }

        return attributeIds;
    }

    private List<String> getAttributeIds(String columnName) {
        List<String> attributeIds = new ArrayList<String>();
        if (getOutputModel() != null) {
            List<ModelAttribute> attributes = getOutputModel().getAttributesByName(columnName);
            if (attributes.size() == 0) {
                throw new SqlException("Column not found in output model and not specified via hint.  Column Name = " + columnName);
            } else {
                for (ModelAttribute modelAttribute : attributes) {
                    attributeIds.add(modelAttribute.getId());
                }
            }
            return attributeIds;
        } else {
            throw new SqlException("No output model was specified for the db reader component.  An output model is required.");
        }
    }

    private String getAttributeId(String tableName, String columnName) {
        if (getOutputModel() != null) {
            ModelAttribute modelAttribute = getOutputModel().getAttributeByName(tableName, columnName);
            if (modelAttribute == null) {
                throw new SqlException("Table and Column not found in output model and not specified via hint.  " + "Table Name = "
                        + tableName + " Column Name = " + columnName);
            }
            return modelAttribute.getId();
        } else {
            throw new SqlException("No output model was specified for the db reader component.  An output model is required.");
        }
    }

    protected void setParamsFromInboundMsgAndRec(Map<String, Object> paramMap, final Message inputMessage, final EntityData entityData) {

        /*
         * input parameters can come from the header and the record. header
         * parms should be used for every record.
         */
        paramMap.clear();
        paramMap.putAll(getParamsFromHeader(inputMessage));
        if (entityData != null) {
            paramMap.putAll(this.getComponent().toRow(entityData, true));
        }
    }

    protected Map<String, Object> getParamsFromHeader(final Message inputMessage) {
        if (inputMessage != null && inputMessage.getHeader() != null) {
            Map<String, Object> paramMap = new HashMap<String, Object>(context.getFlowParameters());
            return paramMap;
        } else {
            return null;
        }
    }

    public Map<Integer, String> getSqlColumnEntityHints(String sql) {
        Map<Integer, String> columnEntityHints = new HashMap<Integer, String>();
        String columns = sql.substring(sql.toLowerCase().indexOf("select") + 6, getFromIndex(sql));
        int commentIdx = 0;
        while (columns.indexOf("/*", commentIdx) != -1) {
            commentIdx = columns.indexOf("/*", commentIdx) + 2;
            int columnIdx = countColumnSeparatingCommas(columns.substring(0, commentIdx)) + 1;
            String entity = StringUtils.trimWhitespace(columns.substring(commentIdx, columns.indexOf("*/", commentIdx)));
            columnEntityHints.put(columnIdx, entity);
        }
        return columnEntityHints;
    }

    protected int countColumnSeparatingCommas(String value) {
        int count = 0;

        int p = 0;
        for (char c : value.toCharArray()) {
            if (c == '(') {
                p++;
            } else if (c == ')') {
                p--;
            } else if (c == ',' && p == 0) {
                count++;
            }
        }
        return count;
    }

    protected int getFromIndex(String sql) {
        sql = sql.toLowerCase();
        int idx = -1;

        idx = sql.toLowerCase().indexOf("from ");
        if (idx == -1) {
            idx = sql.toLowerCase().indexOf("from\n");
        }
        if (idx == -1) {
            idx = sql.toLowerCase().indexOf("from\r\n");
        }
        if (idx == -1) {
            idx = sql.length() - 1;
        }
        return idx;
    }

    protected void logEntityAttributes(EntityData rowData) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        boolean firstEntity = true;
        for (ModelEntity entity : getModelEntities(rowData)) {
            if (firstEntity) {
                firstEntity = false;
            } else {
                sb.append(",\n");
            }
            sb.append("   \"") .append(entity.getName()).append("\" { ");
            boolean firstAttribute = true;
            for (ModelAttribute attribute : entity.getModelAttributes()) {
                if (rowData.containsKey(attribute.getId())) {
                    if (firstAttribute) {
                        firstAttribute = false;
                    } else {
                        sb.append(", ");
                    }
                    sb.append("\"").append(attribute.getName()).append("\"");
                    sb.append(": \"").append(rowData.get(attribute.getId())).append("\"");
                }
            }
            sb.append(" }");
        }
        sb.append("\n}");
        log(LogLevel.DEBUG, sb.toString());
    }
    
    protected Set<ModelEntity> getModelEntities(EntityData rowData) {
        Set<ModelEntity> entities = new LinkedHashSet<ModelEntity>();
        Model model = getOutputModel();
        for (String attributeId : rowData.keySet()) {
            ModelAttribute attribute = model.getAttributeById(attributeId);
            if (attribute != null) {
                ModelEntity entity = model.getEntityById(attribute.getEntityId());
                if (entity != null) {
                    entities.add(entity);
                }
            }
        }
        return entities;
    }
    
    
    
    List<String> getSqls() {
		return sqls;
	}

	long getRowsPerMessage() {
		return rowsPerMessage;
	}

	boolean isTrimColumns() {
		return trimColumns;
	}

	boolean isMatchOnColumnNameOnly() {
		return matchOnColumnNameOnly;
	}

	class ResultSetToEntityDataConverter implements ResultSetExtractor<ArrayList<EntityData>> {
        Message inputMessage;
        
        ISendMessageCallback callback;

        String sqlToExecute;
        
        int outputRecCount;
        
        boolean unitOfWorkLastMessage;
        
        ArrayList<EntityData> payload;

        public ResultSetToEntityDataConverter(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage) {
            this.inputMessage = inputMessage;
            this.callback = callback;
            this.unitOfWorkLastMessage = unitOfWorkLastMessage;
        }

        @Override
        public ArrayList<EntityData> extractData(ResultSet rs) throws SQLException, DataAccessException {
            ResultSetMetaData meta = rs.getMetaData();
            Map<Integer, String> columnHints = getSqlColumnEntityHints(sqlToExecute);
            ArrayList<String> attributeIds = getAttributeIds(meta, columnHints);
            long ts = System.currentTimeMillis();
            while (rs.next()) {
                if (outputRecCount++ % rowsPerMessage == 0 && payload != null) {
                    callback.sendMessage(payload, false);
                    payload = null;
                }
                
                if (payload == null) {
                    payload = new ArrayList<>();
                }

                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);


                EntityData rowData = new EntityData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value = JdbcUtils.getResultSetValue(rs, i);
                    if (trimColumns && value instanceof String) {
                        value = value.toString().trim();
                    }
                    rowData.put(attributeIds.get(i - 1), value);
                }
                payload.add(rowData);
                if (context.getDeployment() != null && context.getDeployment().asLogLevel() == LogLevel.DEBUG) {
                    logEntityAttributes(rowData);
                }
                
                long newTs = System.currentTimeMillis();
                if (newTs - ts > 10000) {
                    getExecutionTracker().updateStatistics(threadNumber, context);
                    ts = newTs;
                }
            }
            return payload;
        }

        public void setSqlToExecute(String sqlToExecute) {
            this.sqlToExecute = sqlToExecute;
        }
    }
}
