package org.jumpmind.metl.core.runtime.resource;

import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;
import org.jumpmind.metl.core.plugin.XMLSetting.Type;
import org.jumpmind.properties.TypedProperties;

@ResourceDefinition(typeName=SMB.TYPE, resourceCategory=ResourceCategory.STREAMABLE)
public class SMB extends AbstractResourceRuntime {

    public static final String TYPE = "SMB";
    
    @SettingDefinition(order=10, required=true, type=Type.TEXT, label="Base SMB URL")
    public static final String SMB_BASE_URL = "smb.base.url";
    
    // User is not required since it can be left blank
    @SettingDefinition(order = 30, required = false, type = Type.TEXT, label="User")
    public static final String SMB_USER = "smb.user";

    // Password is not required since it can be blank.
    @SettingDefinition(order = 40, required = false, type = Type.PASSWORD, label="Password")
    public static final String SMB_PASSWORD = "smb.password";
    
    // Domain is not required since it can be blank.
    @SettingDefinition(order = 50, required = false, type = Type.TEXT, label = "Domain")
    public final static String SMB_DOMAIN = "smb.domain";
    
    IDirectory streamableResource;

    @Override
    protected void start(TypedProperties properties) {
        streamableResource = new SMBDirectory(
                properties.getProperty(SMB_BASE_URL), 
                properties.getProperty(SMB_USER),
                properties.getProperty(SMB_PASSWORD),
                properties.getProperty(SMB_DOMAIN));
    }
    
    @Override
    public void stop() {
        streamableResource.close();
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) streamableResource;
    }

}
