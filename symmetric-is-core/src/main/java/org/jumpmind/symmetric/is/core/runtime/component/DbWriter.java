package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.DmlStatement;
import org.jumpmind.db.sql.DmlStatement.DmlType;
import org.jumpmind.db.sql.ISqlTransaction;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.io.data.writer.DatabaseWriterSettings;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;
import org.jumpmind.symmetric.is.core.runtime.resource.ResourceCategory;

@ComponentDefinition(typeName = DbWriter.TYPE, category = ComponentCategory.WRITER, iconImage="dbwriter.png",
        inputMessage=MessageType.ENTITY_MESSAGE, outgoingMessage=MessageType.NONE, resourceCategory = ResourceCategory.DATASOURCE)
public class DbWriter extends AbstractComponent {

    public static final String TYPE = "Database Writer";

    @SettingDefinition(order = 1, required = false, type = Type.BOOLEAN, label = "Update rows first, then insert if no rows", defaultValue = "false")
    public final static String UPDATE_FIRST = "db.writer.update.first";

    @SettingDefinition(order = 2, required = false, type = Type.BOOLEAN, label = "Quote table and column names", defaultValue = "false")
    public final static String QUOTE_IDENTIFIERS = "db.writer.quote.identifiers";

    @SettingDefinition(order = 3, required = false, type = Type.BOOLEAN, label = "Trim character data to fit within column", defaultValue = "false")
    public final static String FIT_TO_COLUMN = "db.writer.fit.to.column";

    boolean updateFirst = false;

    boolean quoteIdentifiers = false;
    
    boolean fitToColumn = false;
    
    IDatabasePlatform platform;
    
    DatabaseWriterSettings writerSettings;
    
    Map<ModelEntity, DmlStatement> dmlStatements;
    
    Map<ModelEntity, Table> tables;

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        TypedProperties properties = flowStep.getComponent().toTypedProperties(this, false);
        updateFirst = properties.is(UPDATE_FIRST);
        quoteIdentifiers = properties.is(QUOTE_IDENTIFIERS);
        fitToColumn = properties.is(FIT_TO_COLUMN);
        
        DataSource dataSource = (DataSource) resource.reference();
        writerSettings = new DatabaseWriterSettings();
        platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource, new SqlTemplateSettings(), quoteIdentifiers);
        dmlStatements = new HashMap<ModelEntity, DmlStatement>();
        tables = new HashMap<ModelEntity, Table>();

        for (ModelEntity entity : flowStep.getComponent().getInputModel().getModelEntities()) {
            Table table = platform.getTableFromCache(entity.getName(), true);
            ArrayList<String> columnNames = new ArrayList<String>();
            for (ModelAttribute attr: entity.getModelAttributes()) {
                columnNames.add(attr.getName());
            }
            Table targetTable = table.copyAndFilterColumns(columnNames.toArray(new String[columnNames.size()]), null, false);
            dmlStatements.put(entity, platform.createDmlStatement(DmlType.INSERT, targetTable, writerSettings.getTextColumnExpression()));
            tables.put(entity, table);
        }
    }

    @Override
    public void handle(String executionId, final Message inputMessage, final IMessageTarget messageTarget) {

        componentStatistics.incrementInboundMessages();
        
        if (resource == null) {
            throw new RuntimeException("The data source resource has not been configured.  Please configure it.");
        }

        ArrayList<EntityData> inputRows = inputMessage.getPayload();
        if (inputRows == null) {
            return;
        }
        
        ISqlTransaction transaction = platform.getSqlTemplate().startSqlTransaction();
        
        for (EntityData inputRow : inputRows) {
            for (ModelEntity entity : flowStep.getComponent().getInputModel().getModelEntities()) {
                ArrayList<Object> data = new ArrayList<Object>();
                for (ModelAttribute attr: entity.getModelAttributes()) {
                    Object value = inputRow.get(attr.getId());
                    if (fitToColumn && value != null && value instanceof String) {
                        value = fitToColumn(entity, attr, (String) value);
                    }
                    data.add(value);
                }
                DmlStatement st = dmlStatements.get(entity);
                if (log.isDebugEnabled()) {
                    log.debug("Preparing dml: " + st.getSql());
                }
                transaction.prepare(st.getSql());
                
                if (log.isDebugEnabled()) {
                    log.info("Submitting data {} with types {}", Arrays.toString(data.toArray()),
                            Arrays.toString(st.getTypes()));
                }
                transaction.addRow(inputMessage.getHeader().getSequenceNumber(), data.toArray(), st.getTypes());                
            }
        }
        
        transaction.commit();
        transaction.close();
    }
    
    private String fitToColumn(ModelEntity entity, ModelAttribute attr, String value) {
        Table table = tables.get(entity);
        Column column = table.findColumn(attr.getName());
        if (column != null) {
            int size = column.getSizeAsInt();
            if (size > 0 && value.length() > size) {
                value = value.substring(0, size);
            }
        }
        return value;
    }
}
