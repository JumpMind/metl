package org.jumpmind.metl.core.model;

public class PluginRepository extends AbstractObject {

    private static final long serialVersionUID = 1L;

    protected String url;
    
    protected String name;
    
    public PluginRepository() {
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public String getUrl() {
        return url;
    }

}
