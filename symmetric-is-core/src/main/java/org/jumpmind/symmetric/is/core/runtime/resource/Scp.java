package org.jumpmind.symmetric.is.core.runtime.resource;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting.Type;

@ResourceDefinition(typeName=Scp.TYPE, resourceCategory=ResourceCategory.STREAMABLE)
public class Scp extends AbstractResourceRuntime {

    // TODO rename to Scp
    public static final String TYPE = "Scp";

    @SettingDefinition(order=10, required=true, type=Type.TEXT, label="Server")
    public static final String SCP_SERVER = "scp.server";
    
    @SettingDefinition(order=20, required=true, type=Type.INTEGER, label="Port", defaultValue="22")
    public static final String SCP_PORT = "scp.port";
    
    // User is not required since it can be left blank
    @SettingDefinition(type = Type.TEXT, order = 30, required = false, label="User")
    public static final String SCP_USER = "scp.user";

    // Password is not required since it can be blank.
    @SettingDefinition(type = Type.PASSWORD, order = 40, required = false, label="Password")
    public static final String SCP_PASSWORD = "scp.password";    
    
    @SettingDefinition(order = 50, required = true, type = Type.TEXT, label = "Base Path")
    public final static String SCP_BASE_PATH = "scp.base.path";

    @SettingDefinition(type = Type.BOOLEAN, order = 60, required = true, provided = true, defaultValue = "false", label = "Must Exist")
    public static final String SCP_MUST_EXIST = "scp.must.exist";
    
    @SettingDefinition(type = Type.INTEGER, order = 70, required = true, label = "Connection Timeout")
    public static final String SCP_CONNECTION_TIMEOUT = "scp.connection.timeout";
    
    IStreamable streamableResource;

    @Override
    protected void start(TypedProperties properties) {
        streamableResource = new ScpStreamable(resource,
                properties.getProperty(SCP_SERVER), 
                properties.getInt(SCP_PORT),
                properties.getProperty(SCP_USER),
                properties.getProperty(SCP_PASSWORD),
                properties.getProperty(SCP_BASE_PATH),                
                properties.getInt(SCP_CONNECTION_TIMEOUT),
                properties.is(SCP_MUST_EXIST));
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
