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
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.db.sql.SqlException;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ContentMessage;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityData.ChangeType;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.jumpmind.properties.TypedProperties;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.StringUtils;

public class RdbmsReader extends AbstractRdbmsComponentRuntime {

    public static final String TYPE = "RDBMS Reader";

    public final static String TRIM_COLUMNS = "trim.columns";

    public final static String MATCH_ON_COLUMN_NAME_ONLY = "match.on.column.name";
    
    public final static String PASS_INPUT_ROWS_THROUGH = "pass.input.rows.through";        
    
    public final static String RUN_WHEN = "run.when";
    
    public final static String UNIT_OF_WORK = "unit.of.work";

    public static final String COMPONENT_LIFETIME = "PER UNIT OF WORK";
    
    public static final String SQL_SCRIPT = "SQL SCRIPT";
    
    public static final String SQL_STATEMENT = "SQL STATEMENT";    

    List<String> sqls;

    String runWhen = PER_UNIT_OF_WORK;

    long rowsPerMessage = 1000;

    boolean trimColumns = false;

    boolean matchOnColumnNameOnly = false;
    
    boolean passInputRowsThrough = false;

    ChangeType entityChangeType = ChangeType.ADD;

    int rowReadDuringHandle;
    
    String unitOfWork = COMPONENT_LIFETIME;
    
    @Override
    public void start() {
        TypedProperties properties = getTypedProperties();
        sqls = getSqlStatements(true);
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        trimColumns = properties.is(TRIM_COLUMNS);
        matchOnColumnNameOnly = properties.is(MATCH_ON_COLUMN_NAME_ONLY, false);
        passInputRowsThrough = properties.is(PASS_INPUT_ROWS_THROUGH, false);
        runWhen = properties.get(RUN_WHEN, runWhen);
        unitOfWork = properties.get(UNIT_OF_WORK, unitOfWork);
        queryTimeout = properties.getInt(QUERY_TIMEOUT, queryTimeout);
    }

    @Override
    public boolean supportsStartupMessages() {
        return true;
    }

    @Override
    public void handle(final Message inputMessage, final ISendMessageCallback callback, boolean unitOfWorkBoundaryReached) {

        rowReadDuringHandle = 0;

        NamedParameterJdbcTemplate template = getJdbcTemplate();

        int inboundRecordCount = 0;
        Iterator<?> inboundPayload = null;
        if (PER_ENTITY.equals(runWhen) && inputMessage instanceof ContentMessage<?>) {
            inboundPayload = ((Collection<?>) ((ContentMessage<?>) inputMessage).getPayload()).iterator();
            inboundRecordCount = ((Collection<?>) ((ContentMessage<?>) inputMessage).getPayload()).size();
        } else if (PER_MESSAGE.equals(runWhen) && (!(inputMessage instanceof ControlMessage) || context.isStartStep())) {
            inboundPayload = null;
            inboundRecordCount = 1;
        } else if (PER_UNIT_OF_WORK.equals(runWhen) && inputMessage instanceof ControlMessage) {
            inboundPayload = null;
            inboundRecordCount = 1;
        }

        /*
         * A reader can be started by a startup message (if it has no input
         * links) or it can be started by another component that sends messages
         * to it. If the reader is started by another component, then loop for
         * all records in the input message
         */
        ArrayList<EntityData> outboundPayload = new ArrayList<EntityData>(); // =
                                                                             // null;
        for (int i = 0; i < inboundRecordCount; i++) {
            Object entity = inboundPayload != null && inboundPayload.hasNext() ? inboundPayload.next() : null;
            ResultSetToEntityDataConverter resultSetToEntityDataConverter = new ResultSetToEntityDataConverter(inputMessage, callback,
                    unitOfWorkBoundaryReached, outboundPayload);
            if (passInputRowsThrough) {
                outboundPayload.add((EntityData) entity);
            }
            for (String sql : getSqls()) {
                checkForInterruption();
                String sqlToExecute = prepareSql(sql, inputMessage, entity);
                Map<String, Object> paramMap = prepareParams(sqlToExecute, inputMessage, entity, runWhen);
                log(LogLevel.INFO, "About to run: %s", sqlToExecute);
                log(LogLevel.INFO, "Passing params: %s", paramMap);
                resultSetToEntityDataConverter.setSqlToExecute(sqlToExecute);
                template.query(sqlToExecute, paramMap, resultSetToEntityDataConverter);
                if (unitOfWork.equalsIgnoreCase(SQL_STATEMENT)) {
                    sendLeftOverRows(callback, outboundPayload);
                    callback.sendControlMessage();
                }
            }
            if (unitOfWork.equalsIgnoreCase(SQL_SCRIPT)) {
                sendLeftOverRows(callback, outboundPayload);
                callback.sendControlMessage();
            }
        }
        sendLeftOverRows(callback, outboundPayload);
        
    }

    private void sendLeftOverRows(final ISendMessageCallback callback, ArrayList<EntityData> outboundPayload) {
        if (outboundPayload != null && outboundPayload.size() > 0) {
            callback.sendEntityDataMessage(null, outboundPayload);
            outboundPayload.clear();
        } 
    }
    
    private ArrayList<String> getAttributeIds(String sql, ResultSetMetaData meta, Map<Integer, String> sqlEntityHints) throws SQLException {
        ArrayList<String> attributeIds = new ArrayList<String>();
        boolean attributeFound = false;
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

            if (isBlank(tableName)) {
                /*
                 * Some database driver do not support returning the table name
                 * from the metadata. This code attempts to parse the entity
                 * name from the sql
                 */
                tableName = getTableNameFromSql(sql);
            }

           if (matchOnColumnNameOnly) {
                List<String> foundIds = getAttributeIds(columnName);
                if (foundIds.size() == 1) {
                    attributeIds.addAll(foundIds);
                    attributeFound = true;
                } 
                if (foundIds.size() > 1) {
                    throw new MisconfiguredException(String.format("Ambiguous attribute name in model. "
                            + "Cannot match column name to unique attribute. Column: '%s')",columnName));
                }
            } else {
                if (StringUtils.isEmpty(tableName)) {
                    throw new MisconfiguredException("Table name could not be determined from metadata or hints.  Please check column and hint.  "
                            + "(Note to SQL-Server users: metadata may not be returned unless you append 'FOR BROWSE' to the end of your query "
                            + "or set 'useCursors=true' on the JDBC URL.)" + "Query column = " + i);
                }
                String attributeId = getAttributeId(tableName, columnName);
                if (attributeId != null) {
                    attributeFound = true;
                }
                attributeIds.add(attributeId);
            }
        }
        
        if (!attributeFound) {
            throw new MisconfiguredException(String.format("The SQL query results could not be mapped to an existing model entity.  Please verify table columns "
                    + "and hints match the configured output model, '%s'. SQL: '%s')",getOutputModel().getName(),sql));
        }

        return attributeIds;
    }

    protected String getTableNameFromSql(String sql) {
        String tableName = getTableNameFromSql(sql, ' ', ' ');
        if (isBlank(tableName)) {
            tableName = getTableNameFromSql(sql, ' ', '\n');
        }
        if (isBlank(tableName)) {
            tableName = getTableNameFromSql(sql, '\n', ' ');
        }
        if (isBlank(tableName)) {
            tableName = getTableNameFromSql(sql, '\n', '\n');
        }
        if (isBlank(tableName)) {
            tableName = getTableNameFromSql(sql, ' ', '\r');
        }
        if (isBlank(tableName)) {
            tableName = getTableNameFromSql(sql, '\r', ' ');
        }
        if (isBlank(tableName)) {
            tableName = getTableNameFromSql(sql, '\r', '\r');
        }
        return tableName;
    }

    protected String getTableNameFromSql(String sql, char beforeFrom, char afterFrom) {
        String tableName = null;
        int fromIndex = sql.toLowerCase().indexOf(beforeFrom + "from" + afterFrom) + 6;
        if (fromIndex > 5) {
            tableName = sql.substring(fromIndex).trim();
            int nextSpaceIndex = tableName.indexOf(" ");
            if (nextSpaceIndex > 0) {
                tableName = tableName.substring(0, nextSpaceIndex).trim();
            }

            if (tableName.startsWith("\"") || tableName.startsWith("`")) {
                tableName = tableName.substring(1);
            }

            if (tableName.endsWith("\"") || tableName.endsWith("`")) {
                tableName = tableName.substring(0, tableName.length() - 1);
            }
        }
        return tableName;

    }

    private List<String> getAttributeIds(String columnName) {
        List<String> attributeIds = new ArrayList<String>();
        if (getOutputModel() != null) {
            List<ModelAttrib> attributes = getOutputModel().getAttributesByName(columnName);
            if (attributes.size() == 0) {
                throw new SqlException("Column not found in output model and not specified via hint.  Column Name = " + columnName);
            } else {
                for (ModelAttrib modelAttribute : attributes) {
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
            ModelAttrib modelAttribute = getOutputModel().getAttributeByName(tableName, columnName);
            if (modelAttribute != null) {
                return modelAttribute.getId();
            } else {
                return null;
            }
        } else {
            throw new SqlException("No output model was specified for the db reader component.  An output model is required.");
        }
    }

    public static Map<Integer, String> getSqlColumnEntityHints(String sql) {
        Map<Integer, String> columnEntityHints = new HashMap<Integer, String>();
        String columns = sql.substring(sql.toLowerCase().indexOf("select") + 6, getFromIndex(sql));
        int commentIdx = 0;
        Set<String> used = new HashSet<>();
        while (columns.indexOf("/*", commentIdx) != -1) {
            commentIdx = columns.indexOf("/*", commentIdx) + 2;
            int columnIdx = countColumnSeparatingCommas(columns.substring(0, commentIdx)) + 1;
            String entity = StringUtils.trimWhitespace(columns.substring(commentIdx, columns.indexOf("*/", commentIdx)));
            // Only check for dupes if the entity and attributes are provided.
            if (entity.contains(".")) {
                if (!used.contains(entity)) {
                    used.add(entity);
                } else {
                    throw new MisconfiguredException("The same hint was used twice.  "
                            + "Only one column can map to an entity attribute.  "
                            + "The hint that was repeated was for " + entity);
                }
            }
            columnEntityHints.put(columnIdx, entity);
        }
        return columnEntityHints;
    }

    protected static int countColumnSeparatingCommas(String value) {
        int count = 0;
        
        // parenthesis
        int p = 0;
        // quote
        int q = 0;
        for (char c : value.toCharArray()) {
            if (c == '(') {
                p++;
            } else if (c == ')') {
                p--;
            } else if (c == '\'') {
                q++;
            } else if (c == ',' && p == 0 && q%2==0) {
                count++;
            }
        }
        return count;
    }

    protected static int getFromIndex(String sql) {
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
            sb.append("   \"").append(entity.getName()).append("\" { ");
            boolean firstAttribute = true;
            for (ModelAttrib attribute : entity.getModelAttributes()) {
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
            ModelAttrib attribute = model.getAttributeById(attributeId);
            if (attribute != null) {
                ModelEntity entity = model.getEntityById(attribute.getEntityId());
                if (entity != null) {
                    entities.add(entity);
                }
            }
        }
        return entities;
    }

    public void setSqls(List<String> sqls) {
        this.sqls = sqls;
    }

    public void setSql(String sql) {
        this.sqls = new ArrayList<>(1);
        this.sqls.add(sql);
    }

    public void setRowsPerMessage(long rowsPerMessage) {
        this.rowsPerMessage = rowsPerMessage;
    }

    public void setMatchOnColumnNameOnly(boolean matchOnColumnNameOnly) {
        this.matchOnColumnNameOnly = matchOnColumnNameOnly;
    }

    public void setTrimColumns(boolean trimColumns) {
        this.trimColumns = trimColumns;
    }

    public void setEntityChangeType(ChangeType entityChangeType) {
        this.entityChangeType = entityChangeType;
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

    public int getRowReadDuringHandle() {
        return rowReadDuringHandle;
    }

    class ResultSetToEntityDataConverter implements ResultSetExtractor<ArrayList<EntityData>> {
        Message inputMessage;

        ISendMessageCallback callback;

        String sqlToExecute;

        int outputRecCount;

        boolean unitOfWorkLastMessage;
        
        boolean passInputRowsThrough=false;

        ArrayList<EntityData> payload;

        public ResultSetToEntityDataConverter(Message inputMessage, ISendMessageCallback callback, boolean unitOfWorkLastMessage,
                ArrayList<EntityData> payload) {
            this.inputMessage = inputMessage;
            this.callback = callback;
            this.unitOfWorkLastMessage = unitOfWorkLastMessage;
            this.payload = payload;
        }

        @Override
        public ArrayList<EntityData> extractData(ResultSet rs) throws SQLException, DataAccessException {
            checkForInterruption();
            ResultSetMetaData meta = rs.getMetaData();
            Map<Integer, String> columnHints = getSqlColumnEntityHints(sqlToExecute);
            ArrayList<String> attributeIds = getAttributeIds(sqlToExecute, meta, columnHints);
            long ts = System.currentTimeMillis();
            while (rs.next()) {
                if (outputRecCount++ % rowsPerMessage == 0 && payload != null && !payload.isEmpty()) {
                    callback.sendEntityDataMessage(null, payload);
                    payload.clear();
                }

                getComponentStatistics().incrementNumberEntitiesProcessed(threadNumber);

                EntityData rowData = new EntityData();
                rowData.setChangeType(entityChangeType);
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    String attributeId = attributeIds.get(i - 1);
                    if (isNotBlank(attributeId)) {
                        Object value = JdbcUtils.getResultSetValue(rs, i);
                        if (trimColumns && value instanceof String) {
                            value = value.toString().trim();
                        }
                        rowData.put(attributeId, value);
                    }
                }
                rowReadDuringHandle++;
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

    public void setRunWhen(String runWhen) {
        this.runWhen = runWhen;
    }
}
