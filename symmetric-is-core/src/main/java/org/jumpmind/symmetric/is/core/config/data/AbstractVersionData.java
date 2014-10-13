package org.jumpmind.symmetric.is.core.config.data;


public abstract class AbstractVersionData extends AbstractData {

    private static final long serialVersionUID = 1L;

    String versionName;

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getVersionName() {
        return versionName;
    }

}
