package org.jumpmind.metl.ui.init;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.jumpmind.metl.core.model.User;

import com.vaadin.server.VaadinSession;

public class AppSession implements Serializable {

    private static final long serialVersionUID = 1L;

    private static List<AppSession> appSessions = new ArrayList<>();

    public static synchronized List<AppSession> getAppSessions() {
        return new ArrayList<>(appSessions);
    }

    public static synchronized void addAppSession(AppSession appSession) {
        appSessions.add(appSession);
    }

    public static synchronized void remove(AppSession appSession) {
        appSessions.remove(appSession);
    }

    User user;

    String remoteUser;

    String remoteAddress;

    String remoteHost;
    
    String userAgent;

    VaadinSession vaadinSession;

    Date loginTime;

    public AppSession(String remoteUser, String remoteAddress, String remoteHost,
            VaadinSession vaadinSession, String userAgent, Date createTime) {
        this.remoteUser = remoteUser;
        this.remoteAddress = remoteAddress;
        this.remoteHost = remoteHost;
        this.vaadinSession = vaadinSession;
        this.loginTime = createTime;
        this.userAgent = userAgent;
    }
    
    public void setUser(User user) {
        this.user = user;
    }

    public String getLoginId() {
        return user.getLoginId();
    }

    /**
     * @return the remoteUser
     */
    public String getRemoteUser() {
        return remoteUser;
    }

    /**
     * @return the remoteAddress
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     * @return the remoteHost
     */
    public String getRemoteHost() {
        return remoteHost;
    }

    public Date getLastActivity() {
        return new Date(vaadinSession.getLastRequestTimestamp());
    }

    /**
     * @return the createTime
     */
    public Date getLoginTime() {
        return loginTime;
    }
    
    public String getUserAgent() {
        return userAgent;
    }

}
