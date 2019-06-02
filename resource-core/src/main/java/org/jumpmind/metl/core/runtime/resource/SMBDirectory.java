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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.jumpmind.exception.IoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

public class SMBDirectory extends AbstractDirectory {
    
    protected String baseUrl;
    protected String user;
    protected String password;
    protected String domain;
    
    protected static final Logger log = LoggerFactory.getLogger(SMBDirectory.class);
    
    
    public SMBDirectory(
            String baseUrl,
            String user,
            String password,
            String domain) {

        this.baseUrl = baseUrl;
        this.user = user;
        this.password = password;
        this.domain = domain;

        if (!baseUrl.endsWith("/")) {
            this.baseUrl = this.baseUrl + "/";
        }
    }

    @Override
    public FileInfo listFile(String relativePath) {
        return listFile(relativePath, true);
    }

    @Override
    public FileInfo listFile(String relativePath, boolean closeSession) {
        FileInfo fileInfo = null;
        try {
            SmbFile f = new SmbFile(relativePath);
            for (SmbFile file : f.listFiles()) {
                fileInfo = new FileInfo(file.getName(), file.isDirectory(), file.getLastModified(), file.getContentLengthLong());
            }
        } catch (Exception e) {
            throw new IoException(e);
        } finally {
            if (closeSession) {
                close();
            }
        }
        
        return fileInfo;
    }
    
    @Override
    public List<FileInfo> listFiles(boolean closeSession, String... relativePaths) {
        List<FileInfo> fileInfoList =  new ArrayList<>();
        try {
            for (String relativePath : relativePaths) {
                SmbFile[] list = null;
                try {
                    SmbFile dir = new SmbFile(baseUrl + relativePath);
                    list = dir.listFiles();
                } catch (SmbException e) {
                    log.warn("List File Warning ==>" + e.getMessage());
                }
                for (SmbFile f : list) {
                    fileInfoList.add(new FileInfo(f.getName(), f.isDirectory(), f.getLastModified(), f.getContentLength()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failure in listFiles for SFTP.  Error ==> %s",e.getMessage()),e);
        } finally {
            if (closeSession) {
                close();
            }
        }
        
        return fileInfoList;
    }

    @Override
    public void close() {
    }
    
    @Override
    public void close(boolean success) {
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
    
    public void connect() {
    }

    @Override
    public List<FileInfo> listFiles(String... relativePaths) {
        return listFiles(true, relativePaths);
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
        copyToDir(fromFilePath, toDirPath, true);
    }
    
    @Override
    public void copyToDir(String fromFilePath, String toDirPath, boolean closeSession) {
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
    }
    
    @Override
    public void moveToDir(String fromFilePath, String toDirPath, boolean closeSession) {
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        return getInputStream(relativePath, mustExist, true);
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession) {
        InputStream is = null;
        String url = baseUrl + relativePath;
        try {
            is = new SmbFileInputStream(url);
        } catch (Exception e) {
            throw new IoException("Error getting the input stream for SMB "
                    + "endpoint. File: '%s' Error: %s", url, e.getMessage());
        }
        return is; 
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        return getOutputStream(relativePath, mustExist, true, false);
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist, boolean closeSession, boolean append) {
        OutputStream os = null;
        try {
            os = new SmbFileOutputStream(baseUrl + relativePath, append);
        } catch (Exception e) { 
            throw new IoException(e);
        }
        return os;
    }

    @Override
    public boolean delete(String relativePath) {
        return delete(relativePath, true);
    }
    
    @Override
    public boolean delete(String relativePath, boolean closeSession) {
        try {
            SmbFile f = new SmbFile(baseUrl + relativePath);
            f.delete();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public boolean supportsDelete() {
        return true;
    }

    @Override
    public String toString() {
        return baseUrl;
    }
    
    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath, boolean closeSession) {
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath, boolean closeSession) {
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath) {
        return false;
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath, boolean closeSession) {
        return false;
    }    


}
