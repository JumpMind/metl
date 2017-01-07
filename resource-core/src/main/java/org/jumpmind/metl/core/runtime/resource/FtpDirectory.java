/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.core.runtime.resource;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;
import org.jumpmind.exception.IoException;
import org.jumpmind.symmetric.transport.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FtpDirectory extends AbstractDirectory {

    protected static final Logger log = LoggerFactory.getLogger(FtpDirectory.class);

    String hostname;
    Integer port;
    String username;
    String password;
    String basePath;
    Integer connectTimeout;

    public FtpDirectory(String hostname, Integer port, String username, String password, String basePath, Integer connectTimeout) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
        this.basePath = basePath;
        this.connectTimeout = connectTimeout;
    }
    
    @Override
    public FileInfo listFile(String relativePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileInfo listFile(String relativePath, boolean closeSession) {
        return listFile(relativePath);
    }

    @Override
    public List<FileInfo> listFiles(String... relativePaths) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<FileInfo> listFiles(boolean closeSession, String... relativePaths) {
        return listFiles(relativePaths);
    }
    
    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        copyToDir(fromFilePath, toDirPath);
    }
    
    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        moveToDir(fromFilePath, toDirPath);
    }
    
    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath, boolean closeSession) {
        copyFile(fromFilePath, toFilePath);
    }
    
    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath, boolean closeSession) {
        moveFile(fromFilePath, toFilePath);
    }
    
    @Override
    public boolean renameFile(String fileFilePath, String toFilePath) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean renameFile(String fileFilePath, String toFilePath, boolean closeSession) {
        return renameFile(fileFilePath, toFilePath);
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
                throw new RuntimeException(String.format("Failed to connect to %s.  Received a reply code of %d", hostname, reply));
            }

            if (isNotBlank(username)) {
                if (!ftpClient.login(username, password)) {
                    throw new AuthenticationException();
                }
            }

            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();
            ftpClient.addProtocolCommandListener(new ProtocolCommandListener() {
                
                @Override
                public void protocolReplyReceived(ProtocolCommandEvent event) {
                    log.debug("received message: " + event.getMessage().trim() + " reply code:" + event.getReplyCode());
                }
                
                @Override
                public void protocolCommandSent(ProtocolCommandEvent event) {
                    log.debug("sent command: " + event.getCommand() + " message: " + event.getMessage().trim() + " reply code:" + event.getReplyCode());

                }
            });

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
        if (ftpClient != null) {
            try {
                ftpClient.logout();
            } catch (IOException e) {
            }

            try {
                ftpClient.disconnect();
            } catch (IOException e) {
            }
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
        return true;
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        FTPClient ftpClient = null;
        try {
            ftpClient = createClient();
            InputStream is = ftpClient.retrieveFileStream(relativePath);
            if (is != null) {
                return new CloseableInputStream(is, ftpClient);
            } else {
                if (!mustExist) {
                    String msg = String.format("Failed to open %s.  The ftp return code was %s", relativePath, ftpClient.getReplyCode());
                    throw new IoException(msg);
                } else {
                    return null;
                }
            }
        } catch (Exception e) {
            throw new IoException(e);
        }
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession) {
        return getInputStream(relativePath, mustExist);
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(final String relativePath, boolean mustExist) {
        FTPClient ftpClient = null;
        try {
            ftpClient = createClient();
            return new CloseableOutputStream(ftpClient.appendFileStream(relativePath), ftpClient);
        } catch (Exception e) {
            close(ftpClient);
            throw new IoException(e);
        }
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession, boolean append) {
        return getOutputStream(relativePath, mustExist);
    }

    @Override
    public void close() {
    }
    
    @Override
    public void close(boolean success) {
    }

    @Override
    public boolean delete(String relativePath) {
        FTPClient ftpClient = null;
        try {
            ftpClient = createClient();
            return ftpClient.deleteFile(relativePath);
        } catch (Exception e) {
            throw new IoException(e);
        } finally {
            FtpDirectory.this.close(ftpClient);

        }
    }

    @Override
    public boolean delete(String relativePath, boolean closeSession) {
        return delete(relativePath);
    }

    @Override
    public boolean supportsDelete() {
        return true;
    }

    @Override
    public String toString() {
        return String.format("ftp://%s", hostname);
    }

    class CloseableOutputStream extends BufferedOutputStream {
        FTPClient ftpClient;

        public CloseableOutputStream(OutputStream os, FTPClient ftpClient) {
            super(os);
            this.ftpClient = ftpClient;
        }

        @Override
        public void close() throws IOException {
            super.close();
            try {
                int reply = ftpClient.getReply();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    throw new IoException("File transfered failed with a code of " + reply);
                }
            } finally {
                FtpDirectory.this.close(ftpClient);
            }
        }
    }

    class CloseableInputStream extends BufferedInputStream {
        FTPClient ftpClient;

        public CloseableInputStream(InputStream is, FTPClient ftpClient) {
            super(is);
            this.ftpClient = ftpClient;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
                int reply = ftpClient.getReply();
                if (!FTPReply.isPositiveCompletion(reply)) {
                    throw new IoException("File transfered failed with a code of " + reply);
                }
            } catch (Exception ex) {
                log.debug("", ex);
            } finally {
                FtpDirectory.this.close(ftpClient);
            }
        }
    }
    
    @Override
    public void connect() {
    }
}
