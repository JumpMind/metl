package org.jumpmind.symmetric.is.core.runtime.component;

import static org.jumpmind.symmetric.is.core.runtime.component.ComponentSupports.OUTPUT_MESSAGE;
import static org.jumpmind.symmetric.is.core.runtime.component.ComponentSupports.OUTPUT_MODEL;
import static org.jumpmind.symmetric.is.core.runtime.connection.ConnectionCategory.DATASOURCE;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.config.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.MessageManipulationStrategy;
import org.jumpmind.symmetric.is.core.runtime.ShutdownMessage;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.LinkedCaseInsensitiveMap;

@ComponentDefinition(typeName = DbReaderComponent.TYPE, category = ComponentCategory.READER, supports = {
        OUTPUT_MESSAGE, OUTPUT_MODEL }, connectionCategory = DATASOURCE)
public class DbReaderComponent extends AbstractComponent {

	public static final String TYPE="Database Reader";
	
    @SettingDefinition(order = 0, required = true, type = Type.SQL, label = "Sql")
    public final static String SQL = "db.reader.sql";

    @SettingDefinition(order = 10, required = true, type = Type.INTEGER, defaultValue = "1", label = "Rows/Msg")
    public final static String ROWS_PER_MESSAGE = "db.reader.rows.per.message";

    @SettingDefinition(order = 10, required = true, type = Type.BOOLEAN, defaultValue = "false", label = "Trim Columns")
    public final static String TRIM_COLUMNS = "db.reader.trim.columns";

    @SettingDefinition(order = 200, type = Type.CHOICE, choices = { "REPLACE", "ENHANCE" }, defaultValue = "REPLACE", label = "Msg Strategy")
    public final static String MESSAGE_MANIPULATION_STRATEGY = "db.reader.message.manipulation.strategy";

    String sql;
    long rowsPerMessage;
    MessageManipulationStrategy messageManipulationStrategy = MessageManipulationStrategy.REPLACE;
    boolean trimColumns = false;

    @Override
    public void start(IExecutionTracker executionTracker, IConnectionFactory connectionFactory) {
        super.start(executionTracker, connectionFactory);
        applySettings();
    }

    @Override
    public void handle(final Message inputMessage, final IMessageTarget messageTarget) {
    	    	
        DataSource dataSource = this.connectionFactory.create(
                componentNode.getComponentVersion().getConnection()).reference();
        NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
        Map<String, Object> paramMap = new HashMap<String, Object>();
        
        int recordCount=0;
        ArrayList<EntityData> payload = null;
        if (!(inputMessage instanceof StartupMessage) &&
        		!(inputMessage instanceof ShutdownMessage)) {
        	payload = inputMessage.getPayload();
        	recordCount = payload.size();
        }
        
        if (!(inputMessage instanceof ShutdownMessage)) {
            /*
             * A reader can be started by a startup message (if it has no input links) or it 
             * can be started by another component that sends messages to it.  If the reader
             * is started by another component, then loop for all records in the input message
             */
	        for (int i=0;i<=recordCount;i++) {
		        setParamsFromInboundMsgAndRec(paramMap, inputMessage,payload.get(i));
		        template.query(sql, paramMap, new ResultSetExtractor<Object>() {
		            @Override
		            public Object extractData(ResultSet rs) throws SQLException, DataAccessException {
		                Map<Integer, String> sqlEntityHints = getSqlColumnEntityHints(sql);
		                ResultSetMetaData meta = rs.getMetaData();
		                int count = meta.getColumnCount();
		
		                Message message = null;
		                while (rs.next()) {
		                    if (message == null) {
		                        if (messageManipulationStrategy == MessageManipulationStrategy.ENHANCE) {
		                            message = inputMessage.copy();
		                        } else {
		                            message = new Message();
		                            message.setPayload(new ArrayList<EntityData>());
		                        }
		                    }
		                    Map<String, EntityData> records = new LinkedCaseInsensitiveMap<EntityData>(1);
		                    for (int i = 1; i <= count; i++) {
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
		                        if (StringUtils.isBlank(tableName)) {
		                            throw new SQLException(
		                                    "The table name could not be determined while mapping a database record to an EntitiesRow. "
		                                            + "Try using hints to specify a column's table name as part of the SQL query.");
		                        }
		
		                        EntityData record = records.get(tableName);
		                        if (record == null) {
		                            record = new EntityData(tableName);
		                            records.put(tableName, record);
		                        }
		
		                        Object value = JdbcUtils.getResultSetValue(rs, i);
		                        if (trimColumns && value instanceof String) {
		                            value = value.toString().trim();
		                        }
		                        record.put(columnName, value);
		                    }
		
		                    ArrayList<EntityData> payload = inputMessage.getPayload();
		                    payload.addAll(records.values());
		
		                    if (payload.size() >= rowsPerMessage) {
		                        messageTarget.put(message);
		                        message = null;
		                    }
		                }
		
		                if (message != null) {
		                	messageTarget.put(message);
		                }
		                return null;
		            } /* end while for each result set msg from query */
		        });
	        } /* for record count within message */
        } /* if not input message instance of shutdown message */
        /*
         * if this was a startup message, we've done everything we needed to do
         * if this was a shutdown message, we're done
         * in both cases send shutdown message to next node, otherwise keep listening
         */
        if (inputMessage instanceof StartupMessage ||
        		inputMessage instanceof ShutdownMessage) {
        	messageTarget.put(new ShutdownMessage());
        }
    }

    protected void setParamsFromInboundMsgAndRec(Map<String, Object> paramMap, 
    		final Message inputMessage, final EntityData dataRecord) {

    	/*
    	 * input parameters can come from the header and the record.  header parms should be
    	 * used for every record.
    	 */
    	paramMap.clear();
    	paramMap.putAll(getParamsFromHeader(inputMessage));
    	paramMap.putAll(getParamsFromDetailRecord(dataRecord));    
    }

    protected Map<String, Object> getParamsFromHeader(final Message inputMessage) {

    	if (inputMessage != null && inputMessage.getHeader() != null) {
        	Map<String, Object> paramMap = new HashMap<String, Object>(inputMessage.getHeader().getParameters());
        	return paramMap;
        } else {
        	return null;
        }
    }
    
    protected Map<String, Object> getParamsFromDetailRecord(EntityData dataRecord) {

    	return dataRecord;
    }
    
    protected void applySettings() {
        TypedProperties properties = componentNode.getComponentVersion().toTypedProperties(this,
                false);
        sql = properties.get(SQL);
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        messageManipulationStrategy = MessageManipulationStrategy.valueOf(properties
                .get(MESSAGE_MANIPULATION_STRATEGY));
        trimColumns = properties.is(TRIM_COLUMNS);
    }

    protected Map<Integer, String> getSqlColumnEntityHints(String sql) {
        Map<Integer, String> columnEntityHints = new HashMap<Integer, String>();
        String columns = sql.substring(sql.toLowerCase().indexOf("select ") + 7, sql.toLowerCase()
                .indexOf("from "));
        int commentIdx = 0;
        while (columns.indexOf("/*", commentIdx) != -1) {
            commentIdx = columns.indexOf("/*", commentIdx) + 2;
            int columnIdx = StringUtils.countMatches(columns.substring(0, commentIdx), ",") + 1;
            String entity = StringUtils.trim(columns.substring(commentIdx,
                    columns.indexOf("*/", commentIdx)));
            columnEntityHints.put(columnIdx, entity);
        }
        return columnEntityHints;
    }
}
