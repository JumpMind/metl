package org.jumpmind.metl.core.runtime.resource;

import java.io.InputStream;
import java.io.OutputStream;

import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

public class SftpStreamable implements IStreamable {

    protected String server;
    protected Integer port;
    protected String user;
    protected String password;
    protected String basePath;
    protected Integer connectionTimeout;
    protected boolean mustExist;
    
    protected Session session;
    protected ChannelSftp sftp;

    protected static final Logger log = LoggerFactory.getLogger(SftpStreamable.class);

    public SftpStreamable(Resource resource, 
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

    private boolean fileExists(String filePath) {
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
    
    protected void connect() {
        JSch jsch=new JSch();
        try {
            session = jsch.getSession(user, server, port);
            session.setPassword(password.getBytes());
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect(connectionTimeout);
            sftp = (ChannelSftp) session.openChannel("sftp");
            sftp.connect();
        } catch (JSchException e) {
            throw new IoException(e);
        }   
    }    

    @Override
    public InputStream getInputStream(String relativePath, boolean mustExist) {
        try {
            connect();
            String filePath = basePath + relativePath;
            if (mustExist && !fileExists(filePath)) {
                sftp.disconnect();
                session.disconnect();
                throw new IoException("Could not find endpoint %s that was configured as MUST EXIST",filePath);
            }            
            return sftp.get(filePath);
        } catch (Exception e) {
            throw new IoException("Error getting the input stream for ssh endpoint.  Error %s", e.getMessage());
        } 
    }

    @Override
    public boolean supportsOutputStream() {
        return true;
    }

    @Override
    public OutputStream getOutputStream(String relativePath, boolean mustExist) {
        try {
            connect();
            String filePath = basePath + relativePath;
            return sftp.put(filePath, ChannelSftp.OVERWRITE);
        } catch (Exception e) {            
            throw new IoException(e);
        } 
    }

    @Override
    public void close() {
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
    public boolean delete(String relativePath) {
        try {
            connect();
            String filePath = basePath + relativePath;
            sftp.rm(filePath);
            return true;
        } catch (SftpException e) {
            return false;
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
}
