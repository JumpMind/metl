package org.jumpmind.symmetric.is.core.runtime.resource;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Base64;
import org.jumpmind.exception.IoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpStreamable implements IStreamable {

    final Logger log = LoggerFactory.getLogger(getClass());

    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_POST = "POST";

    public static final String SECURITY_NONE = "None";
    public static final String SECURITY_BASIC = "Basic Auth";

    String url;
    String httpMethod;
    String contentType;
    String security;
    String username;
    String password;
    int timeout;
    int contentLength;

    public HttpStreamable(String url, String httpMethod, String contentType, int timeout, String security,
            String username, String password) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.contentType = contentType;
        this.timeout = timeout;
        this.security = security;
        this.username = username;
        this.password = password;
    }

    @Override
    public void open() {
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        try {
            HttpURLConnection httpConnection = buildHttpUrlConnection(relativePath);
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == 200) {
                String type = httpConnection.getContentEncoding();
                InputStream in = httpConnection.getInputStream();
                if (!isBlank(type) && type.equals("gzip")) {
                    in = new GZIPInputStream(in);
                }
                return in;
            } else {
                throw new IoException("Received an unexpected response code of " + responseCode);
            }

        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        HttpURLConnection httpUrlConnection = buildHttpUrlConnection(relativePath);
        return new HttpOutputStream(httpUrlConnection);
    }

    protected HttpURLConnection buildHttpUrlConnection(String relativePath) {
        try {
            String fullUrl = url;
            if (isNotBlank(relativePath)) {
                fullUrl += relativePath;
            }
            HttpURLConnection httpUrlConnection = (HttpURLConnection) new URL(fullUrl).openConnection();
            setBasicAuthIfNeeded(httpUrlConnection);
            if (isNotBlank(contentType)) {
                httpUrlConnection.setRequestProperty("Content-Type", contentType);
            }
            httpUrlConnection.setConnectTimeout(timeout);
            httpUrlConnection.setReadTimeout(timeout);
            httpUrlConnection.setRequestMethod(httpMethod);
            httpUrlConnection.setDoOutput(true);
            httpUrlConnection.setDoInput(true);
            return httpUrlConnection;
        } catch (Exception e) {
            throw new IoException(e);
        }
    }

    protected void setBasicAuthIfNeeded(HttpURLConnection conn) {
        if (SECURITY_BASIC.equals(security)) {
            String userpassword = String.format("%s:%s", username, password);
            String encodedAuthorization = new String(Base64.encodeBase64(userpassword.getBytes()));
            conn.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresContentLength() {
        return false;
    }

    @Override
    public void setContentLength(int length) {
        this.contentLength = length;
    }

    @Override
    public boolean supportsInputStream() {
        return true;
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public boolean delete(String relativePath) {
        return false;
    }

    @Override
    public boolean supportsDelete() {
        return false;
    }
    
    @Override
    public String toString() {
        return url;
    }

    class HttpOutputStream extends OutputStream {

        HttpURLConnection httpUrlConnection;

        OutputStream os;

        public HttpOutputStream(HttpURLConnection httpUrlConnection) {
            this.httpUrlConnection = httpUrlConnection;
            try {
                this.os = this.httpUrlConnection.getOutputStream();
            } catch (IOException e) {
                throw new IoException(e);
            }
        }

        @Override
        public void write(int b) throws IOException {
            this.os.write(b);
        }

        @Override
        public void flush() throws IOException {
            this.os.flush();
        }

        @Override
        public void close() throws IOException {
            this.os.close();
            BufferedReader in = null;
            int responseCode = -1;
            try {
                responseCode = httpUrlConnection.getResponseCode();
                boolean isError = (httpUrlConnection.getResponseCode() >= 400);
                if (isError) {
                    in = new BufferedReader(new InputStreamReader(
                            httpUrlConnection.getErrorStream()));
                } else {
                    in = new BufferedReader(new InputStreamReader(
                            httpUrlConnection.getInputStream()));
                }
                if (log.isDebugEnabled() || isError) {
                    if (isError) {
                        log.warn("Error Response:");
                    }
                    String line = in.readLine();
                    while (line != null) {
                        if (isError) {
                            log.warn(line);
                        } else {
                            log.debug(line);
                        }
                        line = in.readLine();
                    }
                }

            } catch (IOException e) {
                throw new IoException(e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {

                    }
                }
            }
            if (responseCode != 200) {
                throw new IoException("Received an unexpected response code of " + responseCode);
            }

            httpUrlConnection.disconnect();

        }

    }

}
