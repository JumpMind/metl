package org.jumpmind.symmetric.is.core.runtime.connection.localfile;

import java.io.InputStream;
import java.io.OutputStream;

public interface IStreamableConnection {

    public void open();
    
    public boolean requiresContentLength();
    
    public void setContentLength(int length);
    
    public boolean supportsInputStream();
    
    public InputStream getInputStream();
    
    public boolean supportsOutputStream();
    
    public OutputStream getOutputStream();
    
    public void close();
    
    public boolean delete();
    
    public boolean supportsDelete();
    
    public void appendPath(String relativePath, boolean mustExist);
    
    public void resetPath();
    
}
