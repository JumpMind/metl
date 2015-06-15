package org.jumpmind.symmetric.is.core.runtime.component;

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
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.MessageManipulationStrategy;
import org.jumpmind.symmetric.is.core.runtime.StartupMessage;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.util.FormatUtils;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.util.StringUtils;

public class DbReader extends AbstractDbComponent {

    public static final String TYPE = "Database Reader";

    public final static String SQL = "db.reader.sql";

    public final static String ROWS_PER_MESSAGE = "db.reader.rows.per.message";

    public final static String TRIM_COLUMNS = "db.reader.trim.columns";

    public final static String MATCH_ON_COLUMN_NAME_ONLY = "db.reader.match.on.column.name";

    public final static String MESSAGE_MANIPULATION_STRATEGY = "db.reader.message.manipulation.strategy";

    List<String> sqls;

    long rowsPerMessage;

    MessageManipulationStrategy messageManipulationStrategy = MessageManipulationStrategy.REPLACE;

    boolean trimColumns = false;

    boolean matchOnColumnNameOnly = false;

    @Override
    protected void start() {
        applySettings();
    }

    @Override
    public void handle(final Message inputMessage, final IMessageTarget messageTarget) {

        getComponentStatistics().incrementInboundMessages();

        if (getResourceRuntime() == null) {
            throw new RuntimeException("The data source resource has not been configured.  Please configure it.");
        }

        NamedParameterJdbcTemplate template = getJdbcTemplate();
        Map<String, Object> paramMap = new HashMap<String, Object>();

        int inboundRecordCount = 1;
        ArrayList<EntityData> payload = null;
        if (!(inputMessage instanceof StartupMessage)) {
            payload = inputMessage.getPayload();
            if (payload != null) {
                inboundRecordCount = payload.size();
            }
        }

        /*
         * A reader can be started by a startup message (if it has no input
         * links) or it can be started by another component that sends messages
         * to it. If the reader is started by another component, then loop for
         * all records in the input message
         */
        for (int i = 0; i < inboundRecordCount; i++) {
            if (payload != null && payload.size() > i && payload.get(i) instanceof EntityData) {
                setParamsFromInboundMsgAndRec(paramMap, inputMessage, payload.get(i));
            } else {
                setParamsFromInboundMsgAndRec(paramMap, inputMessage, null);
            }

            Message message = null;
            MessageResultSetExtractor messageResultSetExtractor = new MessageResultSetExtractor(inputMessage, messageTarget);
            for (String sql : sqls) {
                String sqlToExecute = FormatUtils.replaceTokens(sql, context.getFlowParametersAsString(), true);
                log(LogLevel.DEBUG, "About to run: " + sqlToExecute);
                messageResultSetExtractor.setSqlToExecute(sqlToExecute);
                message = template.query(sqlToExecute, paramMap, messageResultSetExtractor);
            }
            if (message != null) {
                message.getHeader().setLastMessage(true);
            }
        }
    }

    private Message createMessage(Message inputMessage) {
        Message message;
        if (messageManipulationStrategy == MessageManipulationStrategy.ENHANCE) {
            message = inputMessage.clone(getFlowStepId());
        } else {
            message = inputMessage.clone(getFlowStepId(), new ArrayList<EntityData>());
            message.setPayload(new ArrayList<EntityData>());
        }
        return message;
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
            paramMap.putAll(entityData);
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

    protected void applySettings() {
        TypedProperties properties = getTypedProperties();
        sqls = getSqlStatements(properties.get(SQL));
        rowsPerMessage = properties.getLong(ROWS_PER_MESSAGE);
        messageManipulationStrategy = MessageManipulationStrategy.valueOf(properties.get(MESSAGE_MANIPULATION_STRATEGY,
                messageManipulationStrategy.name()));
        trimColumns = properties.is(TRIM_COLUMNS);
        matchOnColumnNameOnly = properties.is(MATCH_ON_COLUMN_NAME_ONLY, false);
    }

    protected Map<Integer, String> getSqlColumnEntityHints(String sql) {
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
    
    class MessageResultSetExtractor implements ResultSetExtractor<Message> {
        Message inputMessage;
        
        IMessageTarget messageTarget;

        Message message;            

        String sqlToExecute;
        
        int outputRecCount;

        public MessageResultSetExtractor(Message inputMessage, IMessageTarget messageTarget) {
            this.inputMessage = inputMessage;
            this.messageTarget = messageTarget;
        }

        @Override
        public Message extractData(ResultSet rs) throws SQLException, DataAccessException {
            ResultSetMetaData meta = rs.getMetaData();
            Map<Integer, String> columnHints = getSqlColumnEntityHints(sqlToExecute);
            ArrayList<String> attributeIds = getAttributeIds(meta, columnHints);
            while (rs.next()) {
                if (outputRecCount++ % rowsPerMessage == 0 || message == null) {
                    message = createMessage(inputMessage);
                    getComponentStatistics().incrementOutboundMessages();
                    message.getHeader().setSequenceNumber(getComponentStatistics().getNumberOutboundMessages());
                    messageTarget.put(message);
                }
                getComponentStatistics().incrementNumberEntitiesProcessed();

                EntityData rowData = new EntityData();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    Object value = JdbcUtils.getResultSetValue(rs, i);
                    if (trimColumns && value instanceof String) {
                        value = value.toString().trim();
                    }
                    rowData.put(attributeIds.get(i - 1), value);
                }
                ArrayList<EntityData> payload = message.getPayload();
                payload.add(rowData);
                if (context.getDeployment() != null && context.getDeployment().asLogLevel() == LogLevel.DEBUG) {
                    logEntityAttributes(rowData);
                }
            }
            return message;
        }

        public void setSqlToExecute(String sqlToExecute) {
            this.sqlToExecute = sqlToExecute;
        }
    }

}
