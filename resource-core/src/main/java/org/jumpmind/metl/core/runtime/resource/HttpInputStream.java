package org.jumpmind.metl.core.runtime.resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.jumpmind.exception.IoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpInputStream extends InputStream implements IInputStreamWithConnection {

    final Logger log = LoggerFactory.getLogger(getClass());

    HttpURLConnection httpUrlConnection;

    InputStream is;

    StringBuilder response = new StringBuilder();

    public HttpInputStream(HttpURLConnection httpUrlConnection) {
        this.httpUrlConnection = httpUrlConnection;
        try {
            this.is = this.httpUrlConnection.getInputStream();
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public HttpURLConnection getHttpConnection() {
        return httpUrlConnection;
    }

    @Override
    public int read() throws IOException {
        return this.is.read();
    }

    @Override
    public InputStream getInputStream() {
        return is;
    }
}
