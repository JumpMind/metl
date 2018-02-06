package org.jumpmind.metl.core.authentication;

import org.jumpmind.metl.ui.common.ApplicationContext;

public interface IConsoleUserAuthentication {

    public enum AuthenticationStatus {
        VALID,INVALID,LOCKED,EXPIRED
    }
    
    public String getAuthenticationMethod();
    
    public AuthenticationStatus authenticate(String userName, String password
            , ApplicationContext context) throws ConsoleAuthenticationException;
    
//    public boolean validatePasswordComplexity(String password, ApplicationContext context);

}
