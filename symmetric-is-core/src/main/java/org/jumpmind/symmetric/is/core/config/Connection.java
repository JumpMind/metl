package org.jumpmind.symmetric.is.core.config;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.data.ConnectionData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class Connection extends AbstractObject<ConnectionData> {

    private static final long serialVersionUID = 1L;
    List<SettingData> settings;
    
}
