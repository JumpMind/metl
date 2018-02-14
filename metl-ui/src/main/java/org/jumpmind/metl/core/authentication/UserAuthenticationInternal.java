package org.jumpmind.metl.core.authentication;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.core.security.ISecurityService;
import org.jumpmind.metl.ui.common.ApplicationContext;

public class UserAuthenticationInternal implements IConsoleUserAuthentication {

    public final static String AUTHENTICATION_METHOD = "INTERNAL";
    
    public AuthenticationStatus authenticate(String userName, String password, ApplicationContext context) {
        ISecurityService securityService = context.getSecurityService();
        IOperationsService operationsService = context.getOperationsService();
        User user = operationsService.findUserByLoginId(userName);
        String passwordHash = securityService.hash(user.getSalt(), password);
        
        if (user.getPassword() != null && user.getPassword().equals(passwordHash)) {
            
            GlobalSetting expireSetting = context.getOperationsService().findGlobalSetting(GlobalSetting.PASSWORD_EXPIRE_DAYS, 
                    Integer.toString(GlobalSetting.PASSWORD_EXPIRE_DAYS_DEFAULT));
            int passwordExpiresInDays = Integer.parseInt(expireSetting.getValue());
            
            Date expireTime = DateUtils.addDays(new Date(), -passwordExpiresInDays);
            if ( passwordExpiresInDays > 0 
                    && (user.getLastPasswordTime() == null
                    || user.getLastPasswordTime().before(expireTime))) 
            {
                return AuthenticationStatus.EXPIRED;
            }
            return AuthenticationStatus.VALID;
        }
        return AuthenticationStatus.INVALID;
    }

    
    @Override
    public String getAuthenticationMethod() {
        return AUTHENTICATION_METHOD;
    }
    
}

