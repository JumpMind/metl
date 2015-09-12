package org.jumpmind.metl.core.model;

public class ProjectVersionName extends AbstractName {

    private static final long serialVersionUID = 1L;
    
    String versionLabel;
    
    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }
    
    public String getVersionLabel() {
        return versionLabel;
    }
}
