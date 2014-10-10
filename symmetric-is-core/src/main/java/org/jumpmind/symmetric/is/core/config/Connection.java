package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ConnectionData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class Connection extends AbstractObjectWithSettings<ConnectionData> {
    
    private static final long serialVersionUID = 1L;

    public Connection(ConnectionData data, SettingData[] settings) {
        super(data, settings);
    }
    
}
