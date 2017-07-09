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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

public class SftpDirectory extends AbstractDirectory {

    protected String server;
    protected Integer port;
    protected String user;
    protected String password;
    protected String basePath;
    protected String keyFileLocation;
    protected Integer connectionTimeout;
    protected boolean mustExist;
    protected ThreadLocal<Session> threadSession;
    protected ThreadLocal<Map<Integer, ChannelSftp>> threadChannels;
    
    // Define reusable channels
    private static final int CHANNEL_1 = 0;
    private static final int CHANNEL_OUT = 1;
    private static final int CHANNEL_IN  = 2;
    
    protected static final Logger log = LoggerFactory.getLogger(SftpDirectory.class);

    public SftpDirectory(Resource resource, 
            String server,
            Integer port,
            String user,
            String password,
            String keyFileLocation,
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
        this.keyFileLocation = keyFileLocation;
        this.threadSession = new ThreadLocal<Session>();
        this.threadChannels = new ThreadLocal<Map<Integer, ChannelSftp>>();
    }

    @Override
    public FileInfo listFile(String relativePath) {
        return listFile(relativePath, true);
    }
    
    @Override
    public FileInfo listFile(String relativePath, boolean closeSession) {
        ChannelSftp sftp = null;
        FileInfo fileInfo = null;
        try {
            // Get a reusable channel if the session is not auto closed.
            sftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_1);
            sftp.cd(basePath);
        	if (!relativePath.equals(".") && !relativePath.equals("..")) {
            	@SuppressWarnings("rawtypes")
				Vector list = sftp.ls(relativePath);
	            for (Object object : list) {
	                LsEntry entry = (LsEntry)object;
	                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
	                	long updateTime = entry.getAttrs().getMTime();
	                	fileInfo = new FileInfo(relativePath, entry.getAttrs().isDir(), updateTime * 1000, entry.getAttrs().getSize());
	                }
	            }
        	}
    		return fileInfo;
        } catch (Exception e) {
        	return null;
        } finally {
            if (closeSession) {
                close();
            }
        }
    }

    @Override
    public void copyFile(String fromFilePath, String toFilePath) {
        copyFile(fromFilePath, toFilePath, true);
    }
    
    @Override
    public void copyFile(String fromFilePath, String toFilePath, boolean closeSession) {
        ChannelSftp uploadSftp = null;
        InputStream inputStream = null;
        try {
            // Get a reusable channel if the session is not auto closed.
            uploadSftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_1);
            uploadSftp.cd(basePath);
            inputStream = getInputStream(fromFilePath, true);
            uploadSftp.put(inputStream, toFilePath);
        } catch (Exception e) {
            throw new IoException("Error copying file.  Error %s", e.getMessage());
        } finally {
            if (closeSession) {
                close();
            }
        }  
    }

    @Override
    public void moveFile(String fromFilePath, String toFilePath) {
        moveFile(fromFilePath, toFilePath, true);
    }
    
    @Override
    public void moveFile(String fromFilePath, String toFilePath, boolean closeSession) {
        ChannelSftp sftp = null;
        try {
            // Get a reusable channel if the session is not auto closed.
            sftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_1);
            sftp.cd(basePath);
            sftp.rename(fromFilePath, toFilePath);
        } catch (Exception e) {
            throw new IoException("Error moving (renaming) file.  Error %s", e.getMessage());
        } finally {
            if (closeSession) {
                close();
            }
        }
    }

    @Override
    public boolean renameFile(String fromFilePath, String toFilePath) {
        return renameFile(fromFilePath, toFilePath, true);
    }
    
    @Override
    public boolean renameFile(String fromFilePath, String toFilePath, boolean closeSession) {
        ChannelSftp sftp = null;
        try {
            // Get a reusable channel if the session is not auto closed.
            sftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_1);
            sftp.cd(basePath);
            sftp.rename(fromFilePath, toFilePath);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (closeSession) {
                close();
            }
        }
    }    

    private boolean fileExists(ChannelSftp sftp, String filePath) throws SftpException {
        boolean found = false;
        SftpATTRS attributes = null;
        try {
            attributes = sftp.stat(filePath);
        } catch (Exception e) {
            found = false;
        }
        if (attributes != null) {
            found = true;
        }
        return found;
    }
    
    @Override
    public void close() {
        close(true);
    }

    @Override
    public void close(boolean success) {
        Session session = threadSession.get();
        if (session != null) {
            session.disconnect();
            threadSession.set(null);
        }
        Map<Integer, ChannelSftp> channels = threadChannels.get();
        if (channels != null) {
            Iterator<Entry<Integer, ChannelSftp>> itr = channels.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<Integer, ChannelSftp> entry = itr.next();
                ChannelSftp channel = entry.getValue();
                if (channel != null) {
                    channel.disconnect();
                }
            }
            channels.clear();
            threadChannels.set(null);
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
    
    public void connect() {
        JSch jsch=new JSch();
        Session session = null;
        try {
            if (StringUtils.isNotEmpty(keyFileLocation)) {
                jsch.addIdentity(keyFileLocation);
            }
            session = jsch.getSession(user, server, port);
            if (StringUtils.isNotEmpty(password)) {
                session.setPassword(password.getBytes());
            }
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setConfig(config);
            session.connect(connectionTimeout);
            threadSession.set(session);
        } catch (JSchException e) {
            throw new IoException(e);
        }           
    }
    
    protected Session openSession() {
        Session session = threadSession.get();
        if (session == null) {
            session = statelessConnect();
            threadSession.set(session);
        }    
//    	session.sendKeepAliveMsg();
        return session;
    }
    
    
    /**
     * Open and Connect a new SFTP channel.
     * 
     * @return a new channel.
     */
    protected ChannelSftp openConnectedChannel() throws JSchException {
        Session session = openSession();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        try {
            channel.cd(basePath);
        } catch (SftpException e) {
            throw new IoException(e);
        }
        return channel;
    }
    
    
    /**
     * Open and connect a reusable SFTP channel.
     * 
     * @param channelId is the ID of the reusable channel to open.
     * @return a connected reusable channel.
     */
    protected ChannelSftp openConnectedChannel(int channelId) throws JSchException {

        Session session = openSession();
        Map<Integer, ChannelSftp> channels = threadChannels.get();
        if (channels == null) {
            channels = new HashMap<Integer, ChannelSftp>();            
        }
        ChannelSftp channel = channels.get(channelId);
        if (channel == null || channel.isClosed()) {
            channel = (ChannelSftp) session.openChannel("sftp");
        }
        if (!channel.isConnected()) {
            channel.connect();
            try {
                channel.cd(basePath);
            } catch (SftpException e) {
                throw new IoException(e);
            }
        }
        channels.put(channelId, channel);
        threadChannels.set(channels);
        return channel;
    }
    
    protected Session statelessConnect() {
        JSch jsch=new JSch();
        Session session = null;
        try {
            if (StringUtils.isNotEmpty(keyFileLocation)) {
                jsch.addIdentity(keyFileLocation);
            }
            session = jsch.getSession(user, server, port);
            if (StringUtils.isNotEmpty(password)) {
                session.setPassword(password.getBytes());
            }
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
            session.setConfig(config);
            session.connect(connectionTimeout);
            return session;
        } catch (JSchException e) {
            throw new IoException(e);
		}   
    }

    @Override
    public List<FileInfo> listFiles(String... relativePaths) {
        return listFiles(true, relativePaths);
    }
    
    @SuppressWarnings({"unchecked","rawtypes"})
    @Override
    public List<FileInfo> listFiles(boolean closeSession, String... relativePaths) {
        ChannelSftp sftp = null;
        String separator = null;
        List<FileInfo> fileInfoList =  new ArrayList<>();
        try {
            // Get a reusable channel if the session is not auto closed.
            sftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_1);
            sftp.cd(basePath);
            for (String relativePath : relativePaths) {
            	if (!relativePath.equals(".") && !relativePath.equals("..")) {
            	    if (relativePath.isEmpty() || StringUtils.endsWith(relativePath, "/")) {
            	        separator="";
            	    } else {
            	        separator="/";
            	    }
            	    Vector list=new Vector();
	            	try {
                        list.addAll(sftp.ls(relativePath.isEmpty() ? "*" : relativePath));
	            	} catch (SftpException e) {
	            	    log.warn("List File Warning ==>" + e.getMessage());
	            	}
		            for (Object object : list) {
		                LsEntry entry = (LsEntry)object;
		                if (!entry.getFilename().equals(".") && !entry.getFilename().equals("..")) {
		                	long updateTime = entry.getAttrs().getMTime();
	                        fileInfoList.add(new FileInfo(relativePath + separator + entry.getFilename(), entry.getAttrs().isDir(), updateTime * 1000, entry.getAttrs().getSize()));
		                }
		            }
            	}
            }
            return fileInfoList;
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failure in listFiles for SFTP.  Error ==> %s",e.getMessage()),e);
        } finally {
            if (closeSession) {
                close();
            }
        }
    }

    @Override
    public void copyToDir(String fromFilePath, String toDirPath) {
        copyToDir(fromFilePath, toDirPath, true);
    }
    
    @Override
    public void copyToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        ChannelSftp uploadSftp = null;
        FileInfo fileInfo = new FileInfo(fromFilePath, false, new java.util.Date().getTime(), -1);
        InputStream inputStream = null;
        try {
            // Get a reusable channel if the session is not auto closed.
            uploadSftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_1);
            uploadSftp.cd(basePath);
            
            if (!toDirPath.endsWith("/")) {
            	toDirPath += "/";
            }
            inputStream = getInputStream(fromFilePath, true);
            uploadSftp.put(inputStream, toDirPath + fileInfo.getName());
        } catch (Exception e) {
            throw new IoException("Error copying directory.  Error %s", e.getMessage());
        } finally {
            if (closeSession) {
                close();
            }
        }
    }

    @Override
    public void moveToDir(String fromFilePath, String toDirPath) {
        moveToDir(fromFilePath, toDirPath, true);
    }
    
    @Override
    public void moveToDir(String fromFilePath, String toDirPath, boolean closeSession) {
        ChannelSftp sftp = null;
        FileInfo fileInfo = new FileInfo(fromFilePath, false, new java.util.Date().getTime(), -1);
        try {
            // Get a reusable channel if the session is not auto closed.
            sftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_1);
            sftp.cd(basePath);
            if (!toDirPath.endsWith("/")) {
            	toDirPath += "/";
            }
            sftp.rename(fromFilePath, toDirPath + fileInfo.getName());
        } catch (Exception e) {
            throw new IoException("Error moving (renaming) directory.  Error %s", e.getMessage());
        } finally {
            if (closeSession) {
                close();
            }
        }
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        return getInputStream(relativePath, mustExist, true);
    }

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist, boolean closeSession) {
    	Session session = null;
    	ChannelSftp sftp = null;
        try {
        	session = openSession();
            // Get a reusable channel if the session is not auto closed.
            sftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_IN);
            sftp.cd(basePath);
            if (mustExist && !fileExists(sftp, relativePath)) {
                throw new IoException("Could not find endpoint '%s' that was configured as MUST EXIST",relativePath);
            }
            return new CloseableInputStream(sftp.get(relativePath), session, sftp, closeSession);
        } catch (Exception e) {
            if (e instanceof IoException || 
                    (e instanceof SftpException && ((SftpException) e).id != 2)) {
                throw new IoException("Error getting the input stream for sftp endpoint.  Error %s", e.getMessage());
            } else {
                return null;
            }
        } 
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
    	Session session = null;
    	ChannelSftp sftp = null;
        try {
        	session = openSession();
            // Get a reusable channel if the session is not auto closed.
            sftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_OUT);
            createRelativePathDirectoriesIfNecessary(sftp, relativePath, mustExist);
            return new CloseableOutputStream(sftp.put(relativePath, ChannelSftp.OVERWRITE), session, sftp, closeSession);
        } catch (Exception e) {            
            throw new IoException(e);
        } 
    }

    private void createRelativePathDirectoriesIfNecessary(ChannelSftp sftp, String relativePath, boolean mustExist) {
        String[] elements = StringUtils.split(relativePath, "/");
        if (elements.length == 1) {
            // if there is only one element, it's the filename itself. No
            // directories to create
            return;
        } else {
            try {
                for (int i = 0; i < elements.length - 1; i++) {
                    try {
                        sftp.cd(elements[i]);
                    } catch (SftpException cdex) {
                        // if we can't change to the directory, try and create
                        // it
                        sftp.mkdir(elements[i]);
                        sftp.cd(elements[i]);
                    }
                }
                for (int i = 0; i < elements.length - 1; i++) {
                    sftp.cd("../");
                }
            } catch (SftpException mkdirex) {
                log.error("Error writing to Sftp site.  Unable to create relative directory %s.  " + "Error %s", relativePath,
                        mkdirex.getMessage());
                throw new IoException(mkdirex);
            }

        }
    }

    @Override
    public boolean delete(String relativePath) {
        return delete(relativePath, true);
    }
    
    @Override
    public boolean delete(String relativePath, boolean closeSession) {
    	ChannelSftp sftp = null;
        try {
            // Get a reusable channel if the session is not auto closed.
            sftp = (closeSession) ? openConnectedChannel() : openConnectedChannel(CHANNEL_1);
            sftp.cd(basePath);
            sftp.rm(relativePath);
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            if (closeSession) {
                close();
            }
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
        boolean closeSession = true;

        public CloseableOutputStream(OutputStream os, Session session, ChannelSftp sftp, boolean closeSession) {
            super(os);
            this.session = session;
            this.sftp = sftp;
            this.closeSession = closeSession;
        }

        @Override
        public void close() throws IOException {
            super.close();
            if (closeSession) {
                SftpDirectory.this.close();
            }
        }
    }

    class CloseableInputStream extends BufferedInputStream {
        Session session;
        ChannelSftp sftp;
        boolean closeSession = true;

        public CloseableInputStream(InputStream is, Session session, ChannelSftp sftp, boolean closeSession) {
            super(is);
            this.session = session;
            this.sftp = sftp;
            this.closeSession = closeSession;
        }

        @Override
        public void close() throws IOException {
            if (closeSession) {
                try {
                    super.close();
                } catch (Exception ex) {
                    log.debug("", ex);
                } finally {
                    SftpDirectory.this.close();
                }
                super.close();
            }
        }
    }
}
