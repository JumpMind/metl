package org.jumpmind.metl.core.model;

public class PluginRepository extends AbstractNamedObject {

    private static final long serialVersionUID = 1L;

    protected String url;
    
    protected String name;
    
    public PluginRepository(String name, String url) {        
        this.name = name;
        this.url = url;
    }
    
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
