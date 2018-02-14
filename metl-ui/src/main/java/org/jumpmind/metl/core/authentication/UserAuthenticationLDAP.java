package org.jumpmind.metl.core.authentication;

import java.util.Hashtable;

import javax.naming.AuthenticationException;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.apache.commons.lang3.StringUtils;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAuthenticationLDAP implements IConsoleUserAuthentication {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    
    public final static String AUTHENTICATION_METHOD = "LDAP";

    public AuthenticationStatus authenticate(String user, String password, ApplicationContext context) throws ConsoleAuthenticationException {
        IOperationsService operationsService = context.getOperationsService();
        String hostName = operationsService.findGlobalSetting(GlobalSetting.LDAP_HOST).getValue();
        String baseDN = operationsService.findGlobalSetting(GlobalSetting.LDAP_BASE_DN).getValue();
        String searchAtr = operationsService.findGlobalSetting(GlobalSetting.LDAP_SEARCH_ATR).getValue();
        String securityPrincipal = operationsService.findGlobalSetting(GlobalSetting.LDAP_SECURITY_PRINCIPAL, GlobalSetting.LDAP_SECURITY_PRINCIPAL_DEFAULT).getValue();

        boolean result = authenticate(user, password, hostName, baseDN, searchAtr, securityPrincipal);
        if (result) {
            return AuthenticationStatus.VALID;
        } else {
            return AuthenticationStatus.INVALID;
        }
    }

    public boolean authenticate(String user, String password, String hostName, String baseDN, String searchAtr, 
            String securityPrincipal) {
        
        if (StringUtils.isEmpty(hostName)) {
            throw new ConsoleAuthenticationException("LDAP hostName is not configured.");
        } else if (StringUtils.isEmpty(baseDN)) {
            throw new ConsoleAuthenticationException("LDAP baseDN is not configured.");
        } else if (StringUtils.isEmpty(searchAtr)) {
            throw new ConsoleAuthenticationException("LDAP searchAtr is not configured.");
        } else if (StringUtils.isEmpty(password)) {
            throw new ConsoleAuthenticationCredentialException("No Password Provided."); // This code for some reason sails through MS Active directory when no password provided.
        }
        
        if (StringUtils.isEmpty(securityPrincipal)) {
            securityPrincipal = String.format("%s=%s,%s", searchAtr, user, baseDN);
        } else {
            securityPrincipal = StringUtils.replace(securityPrincipal, "${baseDN}", baseDN);    
            securityPrincipal = StringUtils.replace(securityPrincipal, "${searchAttribute}", searchAtr);    
            securityPrincipal = StringUtils.replace(securityPrincipal, "${username}", user);    
        }
        
        try {
            // Set up the environment for creating the initial context
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, 
                    "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, hostName);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, securityPrincipal);
            env.put(Context.SECURITY_CREDENTIALS, password);

            DirContext ctx = new InitialDirContext(env);
            boolean result = ctx != null;
            if (result) {
                ctx.close();
            }
            return result;
        } catch (AuthenticationException ex) {
            logException(ex, user, hostName, baseDN, searchAtr);
            throw new ConsoleAuthenticationCredentialException(ex);
        } catch (CommunicationException ex) {
            logException(ex, user, hostName, baseDN, searchAtr);
            throw new ConsoleAuthenticationConnectionException(ex);
        } catch (NamingException ex) {
            logException(ex, user, hostName, baseDN, searchAtr);
            throw new ConsoleAuthenticationException(ex);
        }
    }

    public boolean authenticate(String user, String password, String hostName, String baseDN, 
            String searchAtr) throws ConsoleAuthenticationException {

        return authenticate(user, password, hostName, baseDN, searchAtr, null);
    }

    protected void logException(Exception ex, String user, String hostName, String baseDN, 
            String searchAtr) {
            logger.warn("LDAP AuthenticationException (hostname= '" + hostName + "',searchAtr= '" + 
                    searchAtr + "', baseDN='" + baseDN + "', user='" + user + "')", ex);            
    }

    @Override
    public String getAuthenticationMethod() {
        return AUTHENTICATION_METHOD;
    }

}
