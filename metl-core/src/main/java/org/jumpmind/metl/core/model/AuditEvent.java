package org.jumpmind.metl.core.model;

public class AuditEvent extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;
    
    public enum EventType { IMPORT, EXPORT, CONFIG, RESTART, LOGIN, CHANGE_DEPENDENCY_VERSION };
    
    String name;
    
    String eventText;
    
    public AuditEvent() {
    }

    public AuditEvent(EventType type, String eventText, String userId) {
        this.name = type.name();
        this.eventText = eventText;
        this.lastUpdateBy = userId;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    public void setEventText(String eventText) {
        this.eventText = eventText;
    }
    
    public String getEventText() {
        return eventText;
    }

}
