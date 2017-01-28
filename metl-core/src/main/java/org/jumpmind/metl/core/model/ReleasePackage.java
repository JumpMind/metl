package org.jumpmind.metl.core.model;

import java.util.Date;

public class ReleasePackage extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;
    
    protected Date releaseDate;
    
    protected boolean released;
    
    protected String name;
    
    protected String versionLabel;

    public ReleasePackage() {
    }

    public ReleasePackage(String name, String versionLabel, Date releaseDate, boolean realeased) {
        super();
        this.name = name;
        this.versionLabel = versionLabel;
        this.releaseDate = releaseDate;
        this.released = realeased;
    }
    
    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }

    @Override
    public void setName(String name) {
        this.name = name;        
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    public void setReleased(boolean realeased) {
        this.released = realeased;
    }
    
    public boolean isReleased() {
        return released;
    }
    
    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }
    
    public Date getReleaseDate() {
        return releaseDate;
    }
    
    public void setVersionLabel(String versionLabel) {
        this.versionLabel = versionLabel;
    }
    
    public String getVersionLabel() {
        return versionLabel;
    }

}
