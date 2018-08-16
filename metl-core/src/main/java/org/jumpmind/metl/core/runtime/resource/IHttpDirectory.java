package org.jumpmind.metl.core.runtime.resource;

public interface IHttpDirectory extends IDirectory {

    public String getHttpMethod();
 
    public String getUrl();
    
    public String getContentType();
    
    public int getTimeout();
    
    public String getSecurity();
    
    public String getUsername();
    
    public String getPassword();    
    
    public String getToken();

}
