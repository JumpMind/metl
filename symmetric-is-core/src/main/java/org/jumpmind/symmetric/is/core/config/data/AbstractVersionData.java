package org.jumpmind.symmetric.is.core.config.data;

import java.util.UUID;


public abstract class AbstractVersionData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String versionName = UUID.randomUUID().toString();

    public void setVersionName(String versionName) {
    	
        this.versionName = versionName;
    }

    public String getVersionName() {
    	
        return versionName;
    }

}
