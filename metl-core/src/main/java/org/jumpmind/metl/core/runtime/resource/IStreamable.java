package org.jumpmind.metl.core.runtime.resource;

import java.io.InputStream;
import java.io.OutputStream;

public interface IStreamable {

    public boolean requiresContentLength();
    
    public void setContentLength(int length);
    
    public boolean supportsInputStream();
    
    public InputStream getInputStream(String relativePath, boolean mustExist);
    
    public boolean supportsOutputStream();
    
    public OutputStream getOutputStream(String relativePath, boolean mustExist);
    
    public void close();
    
    public boolean delete(String relativePath);
    
    public boolean supportsDelete();
        
}
