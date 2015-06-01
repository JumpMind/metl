package org.jumpmind.symmetric.is.core.model;

import java.util.Properties;

import org.apache.commons.lang.StringUtils;

public class MailServer {

    public static final String SETTING_HOST_NAME = "mail.host";
    
    public static final String SETTING_TRANSPORT = "mail.transport";
    
    public static final String SETTING_PORT_NUMBER = "mail.port";
    
    public static final String SETTING_FROM = "mail.from";
    
    public static final String SETTING_USERNAME = "mail.user";
    
    public static final String SETTING_PASSWORD = "mail.password";
    
    public static final String SETTING_USE_TLS = "mail.smtp.starttls.enable";
    
    public static final String SETTING_USE_AUTH = "mail.smtp.auth";
    
    private String hostName;
    
    private String transport;

    private int portNumber;
    
    private String from;
    
    private String username;
    
    private String password;
    
    private boolean useTls;
    
    private boolean useAuth;
    
    public MailServer() {
    }

    public Properties getProperties() {
        Properties prop = new Properties();
        prop.setProperty(MailServer.SETTING_HOST_NAME, StringUtils.trimToEmpty(hostName));
        prop.setProperty(MailServer.SETTING_PORT_NUMBER, String.valueOf(portNumber));
        prop.setProperty(MailServer.SETTING_FROM, StringUtils.trimToEmpty(from));
        prop.setProperty(MailServer.SETTING_USE_TLS, Boolean.toString(useTls));
        prop.setProperty(MailServer.SETTING_USE_AUTH, Boolean.toString(useAuth));
        return prop;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public int getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public boolean isUseAuth() {
        return useAuth;
    }

    public void setUseAuth(boolean useAuth) {
        this.useAuth = useAuth;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

}
