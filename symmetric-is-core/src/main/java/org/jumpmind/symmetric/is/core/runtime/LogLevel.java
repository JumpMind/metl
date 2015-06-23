package org.jumpmind.symmetric.is.core.runtime;

public enum LogLevel {

    DEBUG(10), INFO(20), WARN(30), ERROR(40);
    
    int level;
    
    LogLevel(int level) {
       this.level = level;    
    }
    
    public boolean log (LogLevel level) {
        return this.level <= level.level;
    }
}
