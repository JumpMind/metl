package org.jumpmind.symmetric.is.core.util;

import java.util.Map;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

public class MailSession {

    public static final String SETTING_HOST_NAME = "mail.host";
    
    public static final String SETTING_TRANSPORT = "mail.transport";
    
    public static final String SETTING_PORT_NUMBER = "mail.port";
    
    public static final String SETTING_FROM = "mail.from";
    
    public static final String SETTING_USERNAME = "mail.user";
    
    public static final String SETTING_PASSWORD = "mail.password";
    
    public static final String SETTING_USE_TLS = "mail.smtp.starttls.enable";
    
    public static final String SETTING_USE_AUTH = "mail.smtp.auth";

    Session session;
    
    Map<String, String> globalSettings;
    
    public MailSession(Map<String, String> globalSettings) {
        this.globalSettings = globalSettings;

        Properties prop = new Properties();
        prop.setProperty(SETTING_HOST_NAME, getGlobalSetting(SETTING_HOST_NAME, "localhost"));
        prop.setProperty(SETTING_PORT_NUMBER, getGlobalSetting(SETTING_PORT_NUMBER, "25"));
        prop.setProperty(SETTING_FROM, getGlobalSetting(SETTING_FROM, "symmetricis@localhost"));
        prop.setProperty(SETTING_USE_TLS, getGlobalSetting(SETTING_USE_TLS, "false"));
        prop.setProperty(SETTING_USE_AUTH, getGlobalSetting(SETTING_USE_AUTH, "false"));

        session = Session.getInstance(prop);
    }

    public Transport getTransport() throws MessagingException {
        Transport transport = null;
        transport = session.getTransport(getGlobalSetting(SETTING_TRANSPORT, "smtp"));

        if (Boolean.parseBoolean(getGlobalSetting(SETTING_USE_AUTH, "false"))) {
            transport.connect(globalSettings.get(SETTING_USERNAME), globalSettings.get(SETTING_PASSWORD));
        } else {
            transport.connect();
        }
        return transport;
    }
    
    public void closeTransport(Transport transport) {
        try {
            transport.close();
        } catch (MessagingException e) {
        }
    }

    public Session getSession() {
        return session;
    }

    private String getGlobalSetting(String name, String defaultValue) {
        String value = globalSettings.get(name);
        if (value == null) {
            value = defaultValue;
        }
        return value;
    }

}
