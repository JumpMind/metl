package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.util.ArrayList;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.config.SettingDefinition;
import org.jumpmind.symmetric.is.core.config.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;

@ComponentDefinition(typeName = FormatterComponent.TYPE, category = ComponentCategory.PROCESSOR,
supports = { ComponentSupports.INPUT_MESSAGE, ComponentSupports.INPUT_MODEL, ComponentSupports.OUTPUT_MESSAGE })

public class FormatterComponent extends AbstractComponent {

    public static final String TYPE = "Formatter";

    @SettingDefinition(order = 20, type = Type.CHOICE,
            choices = { FormatType.DELIMITED, FormatType.FIXED_LENGTH, FormatType.XML }, 
            label = "Format Type")
    public final static String FORMATTER_FORMAT_TYPE = "formatter.format.type";

    /* settings */
    String formatType;

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
            
        }
        
    }
    
    private void applySettings() {
        properties = componentNode.getComponentVersion().toTypedProperties(this, false);
        formatType = properties.get(FORMATTER_FORMAT_TYPE);
    }
}
