package org.jumpmind.symmetric.is.core.runtime.resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpOutputStream extends OutputStream {

    final Logger log = LoggerFactory.getLogger(getClass());

    HttpURLConnection httpUrlConnection;

    OutputStream os;

    StringBuilder response = new StringBuilder();

    public HttpOutputStream(HttpURLConnection httpUrlConnection) {
        this.httpUrlConnection = httpUrlConnection;
        try {
            this.os = this.httpUrlConnection.getOutputStream();
        } catch (IOException e) {
            throw new IoException(e);
        }
    }

    public String getResponse() {
        return response.toString();
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
                in = new BufferedReader(new InputStreamReader(httpUrlConnection.getErrorStream()));
            } else {
                in = new BufferedReader(new InputStreamReader(httpUrlConnection.getInputStream()));
            }
            if (isError) {
                log.warn("Error Response:");
            }
            String line = in.readLine();
            while (line != null) {
                response.append(line);
                response.append(System.getProperty("line.separator"));
                line = in.readLine();
            }

        } catch (IOException e) {
            throw new IoException(e);
        } finally {
            IOUtils.closeQuietly(in);
        }
        if (responseCode != 200) {
            throw new IoException(String.format(
                    "Received an unexpected response code of %d with error content of: %s",
                    responseCode, response.toString()));
        }

        httpUrlConnection.disconnect();

    }

}