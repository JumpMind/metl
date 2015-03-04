package org.jumpmind.symmetric.is.core.runtime.component;

import java.util.ArrayList;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(typeName = FormatterComponent.TYPE, category = ComponentCategory.PROCESSOR,
supports = { ComponentSupports.INPUT_MESSAGE, ComponentSupports.INPUT_MODEL, ComponentSupports.OUTPUT_MESSAGE })

public class FormatterComponent extends AbstractComponent {

    public static final String TYPE = "Formatter";

    @SettingDefinition(order = 10, type = Type.CHOICE,
            choices = { FormatType.DELIMITED, FormatType.FIXED_LENGTH, FormatType.XML }, 
            label = "Format Type")
    public final static String FORMATTER_FORMAT_TYPE = "formatter.format.type";
    
    @SettingDefinition(order = 20, required = true, type = Type.STRING, label = "Delimiter")
    public final static String FORMATTER_DELIMITER = "formatter.delimiter";
    
    @SettingDefinition(order = 30, required = true, type = Type.STRING, label = "Quote Character")
    public final static String FORMATTER_QUOTE_CHARACTER = "formatter.quote.character";
    
    @SettingDefinition(type = Type.INTEGER, order = 40, defaultValue = "1",
            label = "Text Rows / Msg")
    public static final String FORMATTER_ROWS_PER_MESSAGE = "formatter.rows.per.message";

    
    /* settings */
    String formatType;
    String delimiter;
    String quoteCharacter;

    /* other vars */
    TypedProperties properties;

    @Override
    public void start(IExecutionTracker executionTracker, IConnectionFactory connectionFactory) {
        super.start(executionTracker, connectionFactory);
        applySettings();
    }

    @Override
    public void handle(Message inputMessage, IMessageTarget messageTarget) {
        
        ArrayList<EntityData> records = inputMessage.getPayload();
        for (EntityData record:records) {
            switch(formatType) {
                case FormatType.DELIMITED: 
            }
        }
        
    }
    
    private void applySettings() {
        properties = componentNode.getComponentVersion().toTypedProperties(this, false);
        formatType = properties.get(FORMATTER_FORMAT_TYPE);
        delimiter = properties.get(FORMATTER_DELIMITER);
        quoteCharacter = properties.get(FORMATTER_QUOTE_CHARACTER);
    }
}
