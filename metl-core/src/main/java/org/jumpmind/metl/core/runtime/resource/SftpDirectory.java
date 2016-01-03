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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SftpDirectory implements IDirectory {

    protected String server;
    protected Integer port;
    protected String user;
    protected String password;
    protected String basePath;
    protected Integer connectionTimeout;
    protected boolean mustExist;
    
    protected static final Logger log = LoggerFactory.getLogger(SftpDirectory.class);

    public SftpDirectory(Resource resource, 
            String server,
            Integer port,
            String user,
            String password,
            String basePath, 
            Integer connectionTimeout,
            boolean mustExist) {
        
        this.server = server;
        this.port = port;
        this.user = user;
        this.password = password;
        this.basePath = basePath;
        this.connectionTimeout = connectionTimeout;
        this.mustExist = mustExist;
    }
    
    @Override
    public FileInfo listFile(String relativePath) {
        Session session = null;
        ChannelSftp sftp = null;
        FileInfo fileInfo = null;
        try {
            session = connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.cd(basePath);

        	if (!relativePath.equals(".") && !relativePath.equals("..")) {
            	@SuppressWarnings("rawtypes")
				Vector list = sftp.ls(relativePath);
	            for (Object object : list) {
	                LsEntry entry = (LsEntry)object;
	                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
	                	fileInfo = new FileInfo(relativePath, entry.getAttrs().isDir(), entry.getAttrs().getMTime(), entry.getAttrs().getSize());
	                }
	            }
        	}
    		return fileInfo;
        } catch (Exception e) {
        	return null;
        } finally {
            SftpDirectory.this.close(session, sftp);
        }
    }
        
    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
        Session session = null;
        ChannelSftp uploadSftp = null;
        ChannelSftp downloadSftp = null;
        try {
            session = connect();
            uploadSftp = (ChannelSftp) session.openChannel("sftp");
            uploadSftp.connect();
            uploadSftp.cd(basePath);
            downloadSftp = (ChannelSftp) session.openChannel("sftp");
            downloadSftp.connect();
            downloadSftp.cd(basePath);
            InputStream inputStream = getInputStream(fromFilePath, true);
            uploadSftp.put(inputStream, toFilePath);
        } catch (Exception e) {
            throw new IoException("Error copying file.  Error %s", e.getMessage());
        } finally {
            SftpDirectory.this.close(session, uploadSftp);
            SftpDirectory.this.close(session, downloadSftp);
        }  
    }
    
    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
        Session session = null;
        ChannelSftp sftp = null;
        try {
            session = connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.cd(basePath);
            sftp.rename(fromFilePath, toFilePath);
        } catch (Exception e) {
            throw new IoException("Error moving (renaming) file.  Error %s", e.getMessage());
        } finally {
            SftpDirectory.this.close(session, sftp);
        }
    }
    
    @Override
    public boolean renameFile(String fromFilePath, String toFilePath) {
        Session session = null;
        ChannelSftp sftp = null;
        try {
            session = connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.cd(basePath);
            sftp.rename(fromFilePath, toFilePath);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            SftpDirectory.this.close(session, sftp);
        }
    }    

    private boolean fileExists(ChannelSftp sftp, String filePath) {
        try {
                SftpATTRS attributes = sftp.stat(filePath);
                if (attributes != null) {
                    return true;
                } else {
                    return false;
                }
        } catch (SftpException e) {
            log.error("Error determing whether a remote file exsits. Error %s", e.getMessage());
            return false;
        }
    }

    protected void close(Session session, ChannelSftp sftp) {
    	if (sftp != null) {
            sftp.disconnect();
            sftp = null;
        }
    	if (session != null) {
    		session.disconnect();
    		session = null;
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
    
    protected Session connect() {
        JSch jsch=new JSch();
        Session session = null;
        try {
            session = jsch.getSession(user, server, port);
            session.setPassword(password.getBytes());
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(connectionTimeout);
            return session;
        } catch (JSchException e) {
            throw new IoException(e);
		}   
    }
    
    @Override
    public List<FileInfo> listFiles(String... relativePaths) {
        Session session = null;
        ChannelSftp sftp = null;
        List<FileInfo> fileInfoList =  new ArrayList<>();
        try {
            session = connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.cd(basePath);
            for (String relativePath : relativePaths) {
            	if (!relativePath.equals(".") && !relativePath.equals("..")) {
	            	@SuppressWarnings("rawtypes")
					Vector list = sftp.ls(relativePath);
		            for (Object object : list) {
		                LsEntry entry = (LsEntry)object;
		                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
		                	fileInfoList.add(new FileInfo(relativePath + entry.getFilename(), entry.getAttrs().isDir(), entry.getAttrs().getMTime(), entry.getAttrs().getSize()));
		                }
		            }
            	}
            }
            return fileInfoList;
        } catch (Exception e) {
            return fileInfoList;
        } finally {
            SftpDirectory.this.close(session, sftp);
        }
    }
    
    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
        Session session = null;
        ChannelSftp uploadSftp = null;
        ChannelSftp downloadSftp = null;
        FileInfo fileInfo = new FileInfo(fromFilePath, false, new java.util.Date().getTime(), -1);
        try {
            session = connect();
            uploadSftp = (ChannelSftp) session.openChannel("sftp");
            uploadSftp.connect();
            uploadSftp.cd(basePath);
            downloadSftp = (ChannelSftp) session.openChannel("sftp");
            downloadSftp.connect();
            downloadSftp.cd(basePath);
            if (!toDirPath.endsWith("/")) {
            	toDirPath += "/";
            }
            InputStream inputStream = getInputStream(fromFilePath, true);
            uploadSftp.put(inputStream, toDirPath + fileInfo.getName());
        } catch (Exception e) {
            throw new IoException("Error copying directory.  Error %s", e.getMessage());
        } finally {
            SftpDirectory.this.close(session, uploadSftp);
            SftpDirectory.this.close(session, downloadSftp);
        }  
    }
    
    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
        Session session = null;
        ChannelSftp sftp = null;
        FileInfo fileInfo = new FileInfo(fromFilePath, false, new java.util.Date().getTime(), -1);
        try {
            session = connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.cd(basePath);
            if (!toDirPath.endsWith("/")) {
            	toDirPath += "/";
            }
            sftp.rename(fromFilePath, toDirPath + fileInfo.getName());
        } catch (Exception e) {
            throw new IoException("Error moving (renaming) directory.  Error %s", e.getMessage());
        } finally {
            SftpDirectory.this.close(session, sftp);
        }
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
    	Session session = null;
    	ChannelSftp sftp = null;
        try {
        	session = connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.cd(basePath);
            if (mustExist && !fileExists(sftp, relativePath)) {
            	SftpDirectory.this.close(session, sftp);
                throw new IoException("Could not find endpoint %s that was configured as MUST EXIST",relativePath);
            }
            return new CloseableInputStreamStream(sftp.get(relativePath), session, sftp);
        } catch (Exception e) {
            throw new IoException("Error getting the input stream for sftp endpoint.  Error %s", e.getMessage());
        } 
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
    	Session session = null;
    	ChannelSftp sftp = null;
        try {
        	session = connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.cd(basePath);
            createRelativePathDirectoriesIfNecessary(sftp, relativePath, mustExist);
            return new CloseableOutputStream(sftp.put(relativePath, ChannelSftp.OVERWRITE), session, sftp);
        } catch (Exception e) {            
            throw new IoException(e);
        } 
    }

    private void createRelativePathDirectoriesIfNecessary(ChannelSftp sftp, String relativePath, boolean mustExist) {
    	String[] elements = StringUtils.split(relativePath,"/");
    	if (elements.length == 1) {
    		//if there is only one element, it's the filename itself.  No directories to create
    		return;
    	} else {
        	for (int i=0;i<elements.length-1;i++) {
        		try {
        			sftp.cd(elements[i]);
        		} catch (SftpException cdex) {
        			//if we can't change to the directory, try and create it
        			try {
        				sftp.mkdir(elements[i]);
        			} catch (SftpException mkdirex) {
        				log.error("Error writing to Sftp site.  Unable to create relative directory %s.  "
        						+ "Error %s",elements[i], mkdirex.getMessage());
        				throw new IoException(mkdirex);
        			}
        		}
        	}
    	}
    }
    
    @Override
    public void close() {
    }

    @Override
    public boolean delete(String relativePath) {
    	Session session = null;
    	ChannelSftp sftp = null;
        try {
            session = connect();
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
            sftp.cd(basePath);
            sftp.rm(relativePath);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
        	SftpDirectory.this.close(session, sftp);
        }
    }
    
    @Override
    public boolean supportsDelete() {
        return true;
    }

    @Override
    public String toString() {
        return basePath;
    }

    class CloseableOutputStream extends BufferedOutputStream {
        Session session;
        ChannelSftp sftp;

        public CloseableOutputStream(OutputStream os, Session session, ChannelSftp sftp) {
            super(os);
            this.session = session;
            this.sftp = sftp;
        }

        @Override
        public void close() throws IOException {
            super.close();
            SftpDirectory.this.close(session, sftp);
        }
    }

    class CloseableInputStreamStream extends BufferedInputStream {
        Session session;
        ChannelSftp sftp;

        public CloseableInputStreamStream(InputStream is, Session session, ChannelSftp sftp) {
            super(is);
            this.session = session;
            this.sftp = sftp;
        }

        @Override
        public void close() throws IOException {
            super.close();
            SftpDirectory.this.close(session, sftp);
        }
    }    
}
