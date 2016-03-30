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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.codec.binary.Base64;
import org.jumpmind.exception.IoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpDirectory implements IDirectory {

    final Logger log = LoggerFactory.getLogger(getClass());

    public static final String HTTP_METHOD_GET = "GET";
    public static final String HTTP_METHOD_PUT = "PUT";
    public static final String HTTP_METHOD_POST = "POST";

    public static final String SECURITY_NONE = "None";
    public static final String SECURITY_BASIC = "Basic Auth";
    public static final String SECURITY_TOKEN = "Token Auth";

    String url;
    String httpMethod;
    String contentType;
    String security;
    String username;
    String password;
    String token;
    int timeout;
    int contentLength;

    public HttpDirectory(String url, String httpMethod, String contentType, int timeout, String security,
            String username, String password, String token) {
        this.url = url;
        this.httpMethod = httpMethod;
        this.contentType = contentType;
        this.timeout = timeout;
        this.security = security;
        this.username = username;
        this.password = password;
        this.token = token;
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
    public boolean renameFile(String fromFilePath, String toFilePath) {
        throw new UnsupportedOperationException();
    }   

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath, boolean closeSession) {
        return renameFile(fromFilePath, toFilePath);
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
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession) {
        return getInputStream(relativePath, mustExist);
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        HttpURLConnection httpUrlConnection = buildHttpUrlConnection(relativePath);
        return new HttpOutputStream(httpUrlConnection);
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession) {
        return getOutputStream(relativePath, mustExist);
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
        } else if (SECURITY_TOKEN.equals(security)) {
        	conn.setRequestProperty("Authorization", "Bearer " + token);
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
    public boolean delete(String relativePath, boolean closeSession) {
        return delete(relativePath);
    }

    @Override
    public boolean supportsDelete() {
        return false;
    }
    
    @Override
    public String toString() {
        return url;
    }

    @Override
    public void connect() {
    }
}
