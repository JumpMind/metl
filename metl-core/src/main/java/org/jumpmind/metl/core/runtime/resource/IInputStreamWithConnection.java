package org.jumpmind.metl.core.runtime.resource;

import java.net.HttpURLConnection;

public interface IInputStreamWithConnection {

    public HttpURLConnection getHttpConnection();
}
