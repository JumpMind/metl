package org.jumpmind.symmetric.is.core.config;

import java.util.Date;
import java.util.HashSet;

public abstract class AbstractVersion {

    VersionId id;    
    
    HashSet<Setting> settings;
    
    Date lastModifyTime;
    
    String lastModifyBy;
    
}
