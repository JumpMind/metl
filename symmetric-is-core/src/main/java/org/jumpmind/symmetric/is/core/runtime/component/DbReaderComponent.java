package org.jumpmind.symmetric.is.core.runtime.component;

import static org.jumpmind.symmetric.is.core.runtime.ComponentSupports.OUTPUT_MESSAGE;
import static org.jumpmind.symmetric.is.core.runtime.ComponentSupports.OUTPUT_MODEL;
import static org.jumpmind.symmetric.is.core.runtime.ConnectionCategory.DATASOURCE;

import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.config.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.ComponentDefinition;
import org.jumpmind.symmetric.is.core.runtime.IComponent;
import org.jumpmind.symmetric.is.core.runtime.IComponentFlowChain;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.MessageManipulationStrategy;


@ComponentDefinition(typeName = "Database Reader", supports = { OUTPUT_MESSAGE, OUTPUT_MODEL }, connectionCategory = DATASOURCE)
public class DbReaderComponent extends AbstractComponent implements IComponent {

    @SettingDefinition(order = 0, required = true, type = Type.STRING, label = "Sql")
    public final static String SQL = "db.reader.sql";

    @SettingDefinition(order = 10, required = true, type = Type.INTEGER, defaultValue = "1", label = "Rows Per Message")
    public final static String ROWS_PER_MESSAGE = "db.reader.rows.per.message";

    @SettingDefinition(order = 10, required = true, type = Type.BOOLEAN, defaultValue = "false", label = "Trim Columns")
    public final static String TRIM_COLUMNS = "db.reader.trim.columns";

    @SettingDefinition(order = 200, type = Type.CHOICE, choices = { "REPLACE", "ENHANCE" }, defaultValue = "REPLACE", label = "Message Manipulation Strategy")
    public final static String MESSAGE_MANIPULATION_STRATEGY = "db.reader.message.manipulation.strategy";

    protected String sql;

    protected long rowsPerMessage;

    protected MessageManipulationStrategy messageManipulationStrategy = MessageManipulationStrategy.REPLACE;

    protected boolean trimColumns = false;

    @Override
    public void start(ComponentFlowNode componentNode, IComponentFlowChain chain) {
    }

    @Override
    public void stop() {
    }

    @Override
    public void handle(Message<?> inputMessage, ComponentFlowNode inputLink) {
    }

}
