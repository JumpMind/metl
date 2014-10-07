package org.jumpmind.symmetric.is.core.config.data;

import java.util.HashSet;

public abstract class AbstractVersionData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String versionId;    
    
    HashSet<SettingData> settings;
    
}
