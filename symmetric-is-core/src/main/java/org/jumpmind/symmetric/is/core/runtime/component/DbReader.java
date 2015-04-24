package org.jumpmind.symmetric.is.core.runtime.component;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.jumpmind.db.sql.SqlException;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.MessageManipulationStrategy;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;
import org.jumpmind.util.FormatUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.StringUtils;

@ComponentDefinition(typeName = DbReader.TYPE, category = ComponentCategory.READER, iconImage="dbreader.png",
        inputMessage=MessageType.ANY,
        outgoingMessage=MessageType.ENTITY,
        resourceCategory = ResourceCategory.DATASOURCE)
public class DbReader extends AbstractComponent {

    public static final String TYPE = "Database Reader";

    @SettingDefinition(order = 0, required = true, type = Type.TEXT, label = "Sql")
    public final static String SQL = "db.reader.sql";

    @SettingDefinition(order = 10, required = true, type = Type.INTEGER, defaultValue = "1",
            label = "Rows/Msg")
    public final static String ROWS_PER_MESSAGE = "db.reader.rows.per.message";

    @SettingDefinition(order = 20, required = true, type = Type.BOOLEAN, defaultValue = "false",
            label = "Trim Columns")
    public final static String TRIM_COLUMNS = "db.reader.trim.columns";
    
    @SettingDefinition(order = 20, required = true, type = Type.BOOLEAN, defaultValue = "false",
            label = "Match On Column Name")
    public final static String MATCH_ON_COLUMN_NAME_ONLY = "db.reader.match.on.column.name";

    @SettingDefinition(order = 200, type = Type.CHOICE, choices = { "REPLACE", "ENHANCE" },
            defaultValue = "REPLACE", label = "Msg Strategy")
    public final static String MESSAGE_MANIPULATION_STRATEGY = "db.reader.message.manipulation.strategy";

    String sql;
    
    long rowsPerMessage;
    
    MessageManipulationStrategy messageManipulationStrategy = MessageManipulationStrategy.REPLACE;
    
    boolean trimColumns = false;
    
    boolean matchOnColumnNameOnly = false;

    @Override
    public void start(String executionId, IExecutionTracker executionTracker) {
        super.start(executionId, executionTracker);
        applySettings();
    }

    @Override
    public void handle( final Message inputMessage, final IMessageTarget messageTarget) {

        componentStatistics.incrementInboundMessages();
        
        if (resource == null) {
            throw new RuntimeException("The data source resource has not been configured.  Please configure it.");
        }
        
        NamedParameterJdbcTemplate template = getJdbcTemplate();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        int inboundRecordCount = 1;
        ArrayList<EntityData> payload = null;
        if (!(inputMessage instanceof StartupMessage)) {
            payload = inputMessage.getPayload();
            inboundRecordCount = payload.size();
        }

        /*
         * A reader can be started by a startup message (if it has no input
         * links) or it can be started by another component that sends messages
         * to it. If the reader is started by another component, then loop for
         * all records in the input message
         */
        for (int i = 0; i < inboundRecordCount; i++) {
            if (payload != null && payload.size() > i) {
                setParamsFromInboundMsgAndRec(paramMap, inputMessage, payload.get(i));
            } else {
                setParamsFromInboundMsgAndRec(paramMap, inputMessage, null);
            }
            
            final String sqlToExecute = FormatUtils.replaceTokens(this.sql, inputMessage.getHeader().getParametersAsString(), true);
            executionTracker.log(executionId, LogLevel.DEBUG, this, "About to run: " + sqlToExecute);
            template.query(sqlToExecute, paramMap, new ResultSetExtractor<Object>() {
                @Override
                public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
                    
                    ResultSetMetaData meta = rs.getMetaData();
                    ArrayList<String> attributeIds=null;
                    Message message = null;
                    int outputRecCount = 0;
                    
                    while (rs.next()) {                    	
                        if (outputRecCount%rowsPerMessage==0 && message != null) {
                            componentStatistics.incrementOutboundMessages();
                            message.getHeader().setSequenceNumber(componentStatistics.getNumberOutboundMessages());
                            messageTarget.put(message);
                            message = null;
                        }
                        
                        componentStatistics.incrementNumberEntitiesProcessed();
                        
                        if (message == null) {
                            message = createMessage(inputMessage);
                        }
                        
                        if (outputRecCount == 0) {
                            attributeIds = getAttributeIds(meta, getSqlColumnEntityHints(sqlToExecute));
                        }
                        
                        EntityData rowData = new EntityData();
                        for (int i = 1; i <= meta.getColumnCount(); i++) {
                            Object value = JdbcUtils.getResultSetValue(rs, i);
                            if (trimColumns && value instanceof String) {
                                value = value.toString().trim();
                            }
                            rowData.put(attributeIds.get(i-1), value);
                        }
                        ArrayList<EntityData> payload = message.getPayload();                        
                        payload.add(rowData);
                        outputRecCount++;
                    } 
                    rs.close();
                    if (message != null) {
                        componentStatistics.incrementOutboundMessages();
                        message.getHeader().setSequenceNumber(componentStatistics.getNumberOutboundMessages());
                        message.getHeader().setLastMessage(true);
                        messageTarget.put(message);
                    }
                    return null;
                } 
            });
        } 
    }

    private Message createMessage(Message inputMessage) {
        Message message;
        if (messageManipulationStrategy == MessageManipulationStrategy.ENHANCE) {
            message = inputMessage.copy(flowStep.getId());
        } else {
            message = new Message(flowStep.getId());
            message.setPayload(new ArrayList<EntityData>());
        }
        return message;
    }
    
    private ArrayList<String> getAttributeIds(ResultSetMetaData meta, Map<Integer, String> sqlEntityHints) throws SQLException {
        
        ArrayList<String> attributeIds = new ArrayList<String>();

        for (int i=1; i<=meta.getColumnCount();i++) {    
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
                    throw new SQLException(
                            "Table name could not be determined from metadata or hints.  Please check column and hint.  "
                                    + "Note that on some databases metadata is only returned if instructed.  "
                                    + "For example, on SQL Server if you append 'FOR BROWSE' on the end of the query metadata will be returned."
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
        if (this.flowStep.getComponent().getOutputModel() != null) {
            List<ModelAttribute> attributes = this.flowStep.getComponent().getOutputModel().getAttributesByName(columnName);
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
        if (this.flowStep.getComponent().getOutputModel() != null) {
        	ModelAttribute modelAttribute = this.flowStep.getComponent().getOutputModel().getAttributeByName(tableName, columnName);
            if (modelAttribute == null) {
                throw new SqlException("Table and Column not found in output model and not specified via hint.  "
                        + "Table Name = " + tableName + " Column Name = " + columnName);
            }        
            return modelAttribute.getId();            
        } else {
            throw new SqlException("No output model was specified for the db reader component.  An output model is required.");
        }
    }

    protected NamedParameterJdbcTemplate getJdbcTemplate() {
        return new NamedParameterJdbcTemplate((DataSource) this.resource.reference());
    }

    protected void setParamsFromInboundMsgAndRec(Map<String, Object> paramMap,
            final Message inputMessage, final EntityData entityData) {

        /*
         * input parameters can come from the header and the record. header
         * parms should be used for every record.
         */
        paramMap.clear();
        paramMap.putAll(getParamsFromHeader(inputMessage));
        if (entityData != null) {
            paramMap.putAll(entityData);
        }
    }

    protected Map<String, Object> getParamsFromHeader(final Message inputMessage) {

        if (inputMessage != null && inputMessage.getHeader() != null) {
            Map<String, Object> paramMap = new HashMap<String, Object>(inputMessage.getHeader()
                    .getParameters());
            return paramMap;
        } else {
            return null;
        }
    }

    protected void applySettings() {
        TypedProperties properties = flowStep.getComponent().toTypedProperties(getSettingDefinitions(false));
        sql = properties.get(SQL);
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        messageManipulationStrategy = MessageManipulationStrategy.valueOf(properties
                .get(MESSAGE_MANIPULATION_STRATEGY));
        trimColumns = properties.is(TRIM_COLUMNS);
        matchOnColumnNameOnly = properties.is(MATCH_ON_COLUMN_NAME_ONLY, false);
    }

    protected Map<Integer, String> getSqlColumnEntityHints(String sql) {
        Map<Integer, String> columnEntityHints = new HashMap<Integer, String>();
        String columns = sql.substring(sql.toLowerCase().indexOf("select") + 6, 
                getFromIndex(sql));
        int commentIdx = 0;
        while (columns.indexOf("/*", commentIdx) != -1) {
            commentIdx = columns.indexOf("/*", commentIdx) + 2;
            int columnIdx = countColumnSeparatingCommas(columns.substring(0, commentIdx)) + 1;
            String entity = StringUtils.trimWhitespace(columns.substring(commentIdx,
                    columns.indexOf("*/", commentIdx)));
            columnEntityHints.put(columnIdx, entity);
        }
        return columnEntityHints;
    }
    
    protected int countColumnSeparatingCommas(String value) {
        int count = 0;
        
        int p = 0;
        for (char c : value.toCharArray()) {
            if (c=='(') {
                p++;
            } else if (c==')') {
                p--;
            } else if (c==',' && p==0) {
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
            idx=sql.length()-1;
        }
        return idx;
    }

}
