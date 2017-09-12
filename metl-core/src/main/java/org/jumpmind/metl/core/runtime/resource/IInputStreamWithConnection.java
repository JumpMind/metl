package org.jumpmind.metl.core.runtime.resource;

import java.io.InputStream;
import java.net.HttpURLConnection;

public interface IInputStreamWithConnection {

    public HttpURLConnection getHttpConnection();
    
    public InputStream getInputStream();
}
