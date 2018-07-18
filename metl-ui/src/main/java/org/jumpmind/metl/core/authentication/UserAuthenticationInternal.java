package org.jumpmind.metl.core.authentication;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
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
        
        if (operationsService.isUserLocked(user)) {
            return AuthenticationStatus.LOCKED;
        } else if (user.getPassword() != null && user.getPassword().equals(passwordHash)) {
        	if (user.getFailedLogins() > 0) {
                user.setFailedLogins(0);
                context.getOperationsService().save(user);
            }
        	
            GlobalSetting expireSetting = context.getOperationsService().findGlobalSetting(GlobalSetting.PASSWORD_EXPIRE_DAYS, 
                    Integer.toString(GlobalSetting.PASSWORD_EXPIRE_DAYS_DEFAULT));
            int passwordExpiresInDays = 0;
            if (!StringUtils.isEmpty(expireSetting.getValue())) {
            	passwordExpiresInDays = Integer.parseInt(expireSetting.getValue());
            }
            
            Date expireTime = DateUtils.addDays(new Date(), -passwordExpiresInDays);
            if ( passwordExpiresInDays > 0 
                    && (user.getLastPasswordTime() == null
                    || user.getLastPasswordTime().before(expireTime))) 
            {
                return AuthenticationStatus.EXPIRED;
            }
            return AuthenticationStatus.VALID;
        }

        GlobalSetting failedAttemptLimitSetting = context.getOperationsService().findGlobalSetting(GlobalSetting.PASSWORD_FAILED_ATTEMPTS, 
                Integer.toString(GlobalSetting.PASSWORD_FAILED_ATTEMPTS_DEFAULT));
        int failedAttemptsLimit = Integer.parseInt(failedAttemptLimitSetting.getValue());
        if (failedAttemptsLimit > 0) {
            user.setFailedLogins(user.getFailedLogins() + 1);
            context.getOperationsService().save(user);
        }
        
        return AuthenticationStatus.INVALID;
    }

    
    @Override
    public String getAuthenticationMethod() {
        return AUTHENTICATION_METHOD;
    }
    
}

