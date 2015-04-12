package org.jumpmind.symmetric.is.core.runtime.component;

public enum MessageType {
        
    NONE(null), ENTITY("E"), TEXT("T"), BINARY("B"), ANY("*");
    
    private String letter;
    
    private MessageType(String letter) {
        this.letter = letter;
    }
    
    public String getLetter() {
        return letter;
    }
    
}
