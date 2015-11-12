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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.db.sql.Row;
import org.jumpmind.db.sql.SqlException;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.EntityData.ChangeType;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
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
    
    public final static String TRIM_COLUMNS = "trim.columns";

    public final static String MATCH_ON_COLUMN_NAME_ONLY = "match.on.column.name";
    
    public final static String RUN_WHEN = "run.when";

    List<String> sqls;
    
    String runWhen = PER_UNIT_OF_WORK;

    long rowsPerMessage = 10000;

    boolean trimColumns = false;

    boolean matchOnColumnNameOnly = false;
    
    ChangeType entityChangeType = ChangeType.ADD;
    
    int rowReadDuringHandle;
    
    @Override
    protected void start() {
        TypedProperties properties = getTypedProperties();
        sqls = getSqlStatements(true);
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        trimColumns = properties.is(TRIM_COLUMNS);
        matchOnColumnNameOnly = properties.is(MATCH_ON_COLUMN_NAME_ONLY, false);
        runWhen = properties.get(RUN_WHEN, runWhen);
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
        if (PER_ENTITY.equals(runWhen) 
                && !(inputMessage instanceof ControlMessage)) {
            inboundPayload = ((Collection<?>)inputMessage.getPayload()).iterator();
            inboundRecordCount = ((Collection<?>)inputMessage.getPayload()).size();
        } else if (PER_MESSAGE.equals(runWhen) && !(inputMessage instanceof ControlMessage)) {
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
        ArrayList<EntityData> outboundPayload = null;
        for (int i = 0; i < inboundRecordCount; i++) {            
            Object entity = inboundPayload != null && inboundPayload.hasNext() ? inboundPayload.next() : null;
            ResultSetToEntityDataConverter resultSetToEntityDataConverter = new ResultSetToEntityDataConverter(inputMessage, callback, unitOfWorkBoundaryReached);
            for (String sql : getSqls()) {
                String sqlToExecute = prepareSql(sql, inputMessage, entity);
                Map<String, Object> paramMap = prepareParams(sqlToExecute, inputMessage, entity);
                log(LogLevel.INFO, "About to run: %s", sqlToExecute);
                log(LogLevel.INFO, "Passing params: %s", paramMap);
                resultSetToEntityDataConverter.setSqlToExecute(sqlToExecute);
                outboundPayload = template.query(sqlToExecute, paramMap, resultSetToEntityDataConverter);                        
            }
        }
        if (outboundPayload != null && outboundPayload.size() > 0) {
            callback.sendMessage(null, outboundPayload);
        }
        
    }
    
    
    protected String prepareSql(String sql, Message inputMessage, Object entity) {
        sql = FormatUtils.replaceTokens(sql, getComponentContext().getFlowParametersAsString(), true);
        sql = FormatUtils.replaceTokens(sql, inputMessage.getHeader().getAsStrings(), true);
        return sql;
    }
    
    protected Map<String, Object> prepareParams(String sql, Message inputMessage, Object entity) {
        Map<String, Object> paramMap = new HashMap<>();
        /*
         * input parameters can come from the header and the record. header
         * parms should be used for every record.
         */
        paramMap.putAll(context.getFlowParameters() == null ? Collections.emptyMap() : context.getFlowParameters());
        paramMap.putAll(inputMessage.getHeader());
        if (entity instanceof EntityData) {
            EntityData entityData = (EntityData)entity;
            paramMap.putAll(this.getComponent().toRow(entityData, true, true));
        } else if (entity != null) {
            paramMap.put("RECORD", entity.toString());
        }
        
        if (PER_MESSAGE.equals(runWhen) && inputMessage.getPayload() instanceof Collection) {
            Collection<?> payload = (Collection<?>)inputMessage.getPayload();
            enhanceParamMapWithInValues(paramMap, payload, sql);
        }
        return paramMap;
    }
    
    protected void enhanceParamMapWithInValues(Map<String, Object> paramMap, Collection<?> payload, String sql) {
        Set<String> attributeNames = findWhereInParameters(sql);
        for (String attributeName : attributeNames) {
            List<Object> in = new ArrayList<>();
            Iterator<?> i = payload.iterator();
            while (i.hasNext()) {
                Object next = i.next();
                if (next instanceof EntityData) {
                    EntityData entityData = (EntityData)next;
                    Row row = this.getComponent().toRow(entityData, true, true);
                    Object value = row.get(attributeName);
                    if (value != null) {
                        in.add(value);
                    }
                }
            }
            paramMap.put(attributeName, in);
        }
    }
    
    protected Set<String> findWhereInParameters(String sql) {
        Set<String> inAttributeNames = new HashSet<>();
        List<Integer> indexes = new ArrayList<>();
        int currentIndex = -4;
        do {
          int index = sql.indexOf(" in ", currentIndex+4);
          if (index < 0) {
              index = sql.indexOf(" IN ", currentIndex+4);
          }
          currentIndex = index;
          if (currentIndex > 0) {
              indexes.add(currentIndex);
              int left = sql.indexOf("(", currentIndex+4);
              int right = sql.indexOf(")", currentIndex+4);
              if (left > 0 && right > 0) {
                  String attributeId = sql.substring(left+1, right).trim();
                  attributeId = attributeId.substring(1);
                  inAttributeNames.add(attributeId);
              }              
          }
        } while (currentIndex > 0 && currentIndex < sql.length());
        
        return inAttributeNames;
    }

    private ArrayList<String> getAttributeIds(String sql, ResultSetMetaData meta, Map<Integer, String> sqlEntityHints) throws SQLException {
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
            
            if (isBlank(tableName)) {
                /*
                 * Some database driver do not support returning the table name from the
                 * metadata.  This code attempts to parse the entity name from the sql
                 */
                int fromIndex = sql.toLowerCase().indexOf(" from ")+6;
                if (fromIndex > 0) {
                    tableName = sql.substring(fromIndex).trim();
                    int nextSpaceIndex = tableName.indexOf(" ");
                    if (nextSpaceIndex > 0) {
                        tableName = tableName.substring(0, nextSpaceIndex).trim();
                    }
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
                attributeIds.add(getAttributeId(tableName, columnName));
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
            if (modelAttribute != null) {
                return modelAttribute.getId();
            } else {
                return null;
            }            
        } else {
            throw new SqlException("No output model was specified for the db reader component.  An output model is required.");
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
            ArrayList<String> attributeIds = getAttributeIds(sqlToExecute, meta, columnHints);
            long ts = System.currentTimeMillis();
            while (rs.next()) {
                if (outputRecCount++ % rowsPerMessage == 0 && payload != null) {
                    callback.sendMessage(null, payload);
                    payload = null;
                }
                
                if (payload == null) {
                    payload = new ArrayList<>();
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
}
