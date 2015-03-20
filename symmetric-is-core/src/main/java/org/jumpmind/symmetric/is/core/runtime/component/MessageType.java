package org.jumpmind.symmetric.is.core.runtime.component;

public enum MessageType {
        
    NONE(null), ENTITY_MESSAGE("E"), TEXT_MESSAGE("T"), BINARY_MESSAGE("B");
    
    private String letter;
    
    private MessageType(String letter) {
        this.letter = letter;
    }
    
    public String getLetter() {
        return letter;
    }
    
}
