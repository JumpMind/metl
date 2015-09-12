package org.jumpmind.metl.core.runtime.resource;

import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.properties.TypedProperties;

@ResourceDefinition(typeName=Sftp.TYPE, resourceCategory=ResourceCategory.STREAMABLE)
public class Sftp extends AbstractResourceRuntime {

    // TODO rename to Sftp
    public static final String TYPE = "Sftp";

    @SettingDefinition(order=10, required=true, type=Type.TEXT, label="Server")
    public static final String SFTP_SERVER = "sftp.server";
    
    @SettingDefinition(order=20, required=true, type=Type.INTEGER, label="Port", defaultValue="22")
    public static final String SFTP_PORT = "sftp.port";
    
    // User is not required since it can be left blank
    @SettingDefinition(type = Type.TEXT, order = 30, required = false, label="User")
    public static final String SFTP_USER = "sftp.user";

    // Password is not required since it can be blank.
    @SettingDefinition(type = Type.PASSWORD, order = 40, required = false, label="Password")
    public static final String SFTP_PASSWORD = "sftp.password";    
    
    @SettingDefinition(order = 50, required = true, type = Type.TEXT, label = "Base Path")
    public final static String SFTP_BASE_PATH = "sftp.base.path";

    @SettingDefinition(type = Type.BOOLEAN, order = 60, required = true, provided = true, defaultValue = "false", label = "Must Exist")
    public static final String SFTP_MUST_EXIST = "sftp.must.exist";
    
    @SettingDefinition(type = Type.INTEGER, order = 70, required = false, defaultValue="30000", label = "Connection Timeout")
    public static final String SFTP_CONNECTION_TIMEOUT = "sftp.connection.timeout";
    
    IStreamable streamableResource;

    @Override
    protected void start(TypedProperties properties) {
        streamableResource = new SftpStreamable(resource,
                properties.getProperty(SFTP_SERVER), 
                properties.getInt(SFTP_PORT),
                properties.getProperty(SFTP_USER),
                properties.getProperty(SFTP_PASSWORD),
                properties.getProperty(SFTP_BASE_PATH),                
                properties.getInt(SFTP_CONNECTION_TIMEOUT),
                properties.is(SFTP_MUST_EXIST));
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
