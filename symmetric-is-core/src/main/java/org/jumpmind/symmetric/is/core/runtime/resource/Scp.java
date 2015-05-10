package org.jumpmind.symmetric.is.core.runtime.resource;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;

@ResourceDefinition(typeName=Scp.TYPE, resourceCategory=ResourceCategory.STREAMABLE)
public class Scp extends AbstractResourceRuntime {

    public static final String TYPE = "SSH File System";

    @SettingDefinition(order=10, required=true, type=Type.STRING, label="Server")
    public static final String SSH_SERVER = "ssh.server";
    
    @SettingDefinition(order=20, required=true, type=Type.INTEGER, label="Port", defaultValue="22")
    public static final String SSH_PORT = "ssh.port";
    
    // User is not required since it can be left blank
    @SettingDefinition(type = Type.STRING, order = 30, required = false, label="User")
    public static final String SSH_USER = "ssh.user";

    // Password is not required since it can be blank.
    @SettingDefinition(type = Type.PASSWORD, order = 40, required = false, label="Password")
    public static final String SSH_PASSWORD = "ssh.password";    
    
    @SettingDefinition(order = 50, required = true, type = Type.STRING, label = "Base Path")
    public final static String SSH_BASE_PATH = "ssh.base.path";

    @SettingDefinition(type = Type.BOOLEAN, order = 60, required = true, provided = true, defaultValue = "false", label = "Must Exist")
    public static final String SSH_MUST_EXIST = "ssh.must.exist";
    
    @SettingDefinition(type = Type.INTEGER, order = 70, required = true, label = "Connection Timeout")
    public static final String SSH_CONNECTION_TIMEOUT = "ssh.connection.timeout";
    
    IStreamable streamableResource;

    @Override
    protected void start(TypedProperties properties) {
        streamableResource = new ScpStreamable(resource,
                properties.getProperty(SSH_SERVER), 
                properties.getInt(SSH_PORT),
                properties.getProperty(SSH_USER),
                properties.getProperty(SSH_PASSWORD),
                properties.getProperty(SSH_BASE_PATH),                
                properties.getInt(SSH_CONNECTION_TIMEOUT),
                properties.is(SSH_MUST_EXIST));
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
