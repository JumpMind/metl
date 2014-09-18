package org.jumpmind.symmetric.is.core.config.data;

import java.util.Date;
import java.util.HashSet;

public abstract class AbstractVersionData {

    String versionId;    
    
    HashSet<SettingData> settings;
    
    Date lastModifyTime;
    
    String lastModifyBy;
    
}
