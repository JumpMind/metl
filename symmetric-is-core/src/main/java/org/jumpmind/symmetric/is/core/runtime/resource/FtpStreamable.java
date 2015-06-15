package org.jumpmind.symmetric.is.core.runtime.resource;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.transport.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FtpStreamable implements IStreamable {

    protected static final Logger log = LoggerFactory.getLogger(FtpStreamable.class);

    String hostname;
    Integer port;
    String username;
    String password;
    String basePath;
    Integer connectTimeout;

    public FtpStreamable(String hostname, Integer port, String username, String password, String basePath, Integer connectTimeout) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.basePath = basePath;
        this.connectTimeout = connectTimeout;
    }

    protected FTPClient createClient() {
        FTPClient ftpClient = new FTPClient();
        FTPClientConfig config = new FTPClientConfig();
        ftpClient.configure(config);

        if (connectTimeout != null) {
            ftpClient.setConnectTimeout(connectTimeout);
        }

        try {
            if (port != null) {
                ftpClient.connect(hostname, port);
            } else {
                ftpClient.connect(hostname);
            }

            int reply = ftpClient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) {
                throw new RuntimeException(String.format("Failed to connect to %s.  Recevied a reply code of %d", hostname, reply));
            }

            if (isNotBlank(username)) {
                if (!ftpClient.login(username, password)) {
                    throw new AuthenticationException();
                }
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            if (isNotBlank(basePath)) {
                ftpClient.changeWorkingDirectory(basePath);
            }
            return ftpClient;
        } catch (Exception e) {
            close();
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            } else {
                throw new IoException(e);
            }
        }

    }

    protected void close(FTPClient ftpClient) {
        try {
            ftpClient.logout();
        } catch (IOException e) {
        }

        try {
            ftpClient.disconnect();
        } catch (IOException e) {
        }
    }

    @Override
    public boolean requiresContentLength() {
        return false;
    }

    @Override
    public void setContentLength(int length) {
    }

    @Override
    public boolean supportsInputStream() {
        return false;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        return null;
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(final String relativePath, boolean mustExist) {
        try {
            final FTPClient ftpClient = createClient();
            return new BufferedOutputStream(ftpClient.appendFileStream(relativePath)) {
                @Override
                public void close() throws IOException {
                    try {
                        super.close();
//                        if (!ftpClient.completePendingCommand()) {
//                            throw new IoException("Error transfering %s", relativePath);
//                        }
                    } finally {
                        FtpStreamable.this.close(ftpClient);
                    }
                }
            };
        } catch (Exception e) {
            throw new IoException(e);
        }
    }

    @Override
    public void close() {

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
        return String.format("ftp://%s", hostname);
    }
}
