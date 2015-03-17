package org.jumpmind.symmetric.is.core.runtime.component;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.csv.CsvWriter;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.IExecutionTracker;
import org.jumpmind.symmetric.is.core.runtime.Message;
import org.jumpmind.symmetric.is.core.runtime.flow.IMessageTarget;
import org.jumpmind.symmetric.is.core.runtime.resource.IResourceFactory;

@ComponentDefinition(typeName = DelimitedFormatter.TYPE, category = ComponentCategory.PROCESSOR, iconImage="format.png",
supports = { ComponentSupports.INPUT_MESSAGE, ComponentSupports.INPUT_MODEL, ComponentSupports.OUTPUT_MESSAGE })

public class DelimitedFormatter extends AbstractComponent {

    public static final String TYPE = "Delimited Formatter";

    @SettingDefinition(order = 10, required = true, type = Type.STRING, label = "Delimiter")
    public final static String FORMATTER_DELIMITER = "formatter.delimiter";
    
    @SettingDefinition(order = 20, required = true, type = Type.STRING, label = "Quote Character")
    public final static String FORMATTER_QUOTE_CHARACTER = "formatter.quote.character";
    
    /* settings */
    String formatType;
    String delimiter;
    String quoteCharacter;

    /* other vars */
    TypedProperties properties;

    @Override
    public void start(IExecutionTracker executionTracker, IResourceFactory resourceFactory) {
        super.start(executionTracker, resourceFactory);
        applySettings();
    }

    @Override
    public void handle(String executionId, Message inputMessage, IMessageTarget messageTarget) {
        
        componentStatistics.incrementInboundMessages();
        ArrayList<ArrayList<EntityData>> inputRows = inputMessage.getPayload();

        Message outputMessage = new Message(flowStep.getId());
        ArrayList<String> outputPayload = new ArrayList<String>(); 
        
        //for every row in the input message
        String outputRec;
        for (ArrayList<EntityData> inputRow : inputRows) {
            outputRec = processInputRow(inputRow);
            outputPayload.add(outputRec);
        } 
        outputMessage.setPayload(outputPayload);
        messageTarget.put(outputMessage);
    }
    
    private String processInputRow(ArrayList<EntityData> inputRow) {
   
        Writer writer = new StringWriter();
        CsvWriter csvWriter = new CsvWriter(writer, delimiter.charAt(0));
        csvWriter.setTextQualifier(quoteCharacter.charAt(0));
        
        //get attribute settings
        //sort them by ordinal position
        //loop through them in ordinal position, appending the column content
        //csvWriter.write(content);
        
        //csvWriter.endRecord();
        return writer.toString();
    }
    
    private void applySettings() {
        properties = flowStep.getComponentVersion().toTypedProperties(this, false);
        delimiter = properties.get(FORMATTER_DELIMITER);
        quoteCharacter = properties.get(FORMATTER_QUOTE_CHARACTER);
    }
}
