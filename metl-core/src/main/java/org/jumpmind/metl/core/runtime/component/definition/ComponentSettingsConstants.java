package org.jumpmind.metl.core.runtime.component.definition;

final public class ComponentSettingsConstants {

    private ComponentSettingsConstants() {
    }
    
    public final static String INBOUND_QUEUE_CAPACITY = "inbound.queue.capacity";     
    
    public final static String ENABLED = "enabled";

    public final static String LOG_INPUT = "logInput";

    public final static String LOG_OUTPUT = "logOutput";
    
    public final static String NOTES = "notes";
    
    public final static String ROWS_PER_MESSAGE = "rows.per.message";
    
    public final static String RUN_WHEN = "run.when";
    
    public static final String PER_UNIT_OF_WORK = "PER UNIT OF WORK";

    public static final String PER_MESSAGE = "PER MESSAGE";

    public static final String PER_ENTITY = "PER ENTITY"; 
    
    public static final String FORMAT = "format";
    public static final String FORMAT_AUTOMATIC = "AUTOMATIC";
    public static final String FORMAT_JSON = "JSON";
    public static final String FORMAT_XML = "XML";

    public static final String STRUCTURE = "structure";
    public static final String STRUCTURE_BY_INBOUND_ROW = "BY_INBOUND_ROW";
    public static final String STRUCTURE_BY_TABLE = "BY_TABLE";

}
