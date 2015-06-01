package org.jumpmind.symmetric.is.core.runtime.resource;

import static org.jumpmind.symmetric.is.core.runtime.resource.HttpStreamable.HTTP_METHOD_GET;
import static org.jumpmind.symmetric.is.core.runtime.resource.HttpStreamable.HTTP_METHOD_POST;
import static org.jumpmind.symmetric.is.core.runtime.resource.HttpStreamable.HTTP_METHOD_PUT;
import static org.jumpmind.symmetric.is.core.runtime.resource.HttpStreamable.SECURITY_BASIC;
import static org.jumpmind.symmetric.is.core.runtime.resource.HttpStreamable.SECURITY_NONE;

import org.jumpmind.properties.TypedProperties;
import org.jumpmind.symmetric.is.core.model.SettingDefinition;
import org.jumpmind.symmetric.is.core.model.SettingDefinition.Type;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;

@ResourceDefinition(typeName = Http.TYPE, resourceCategory = ResourceCategory.STREAMABLE)
public class Http extends AbstractResourceRuntime {

    public static final String TYPE = "Http";

    @SettingDefinition(type = Type.TEXT, order = 10, required = true, label = "URL")
    public static final String URL = "url";

    @SettingDefinition(
            type = Type.CHOICE,
            choices = { HTTP_METHOD_GET, HTTP_METHOD_PUT, HTTP_METHOD_POST },
            order = 20,
            defaultValue = HTTP_METHOD_GET,
            required = true,
            label = "Http Method")
    public static final String HTTP_METHOD = "http.method";
    
    
    @SettingDefinition(
            type = Type.MULTILINE_TEXT,
            order = 25,
            defaultValue = "text/xml; charset=utf-8",
            required = true,
            label = "Content Type")
    public static final String CONTENT_TYPE = "content.type";

    @SettingDefinition(
            type = Type.INTEGER,
            order = 25,
            required = true,
            label = "Http Timeout",
            defaultValue = "60000")
    public static final String HTTP_TIMEOUT = "http.timeout";

    @SettingDefinition(
            type = Type.CHOICE,
            order = 30,
            choices = { SECURITY_NONE, SECURITY_BASIC },
            defaultValue = SECURITY_NONE,
            label = "Security")
    public static final String SECURITY = "security.type";

    @SettingDefinition(type = Type.TEXT, order = 40, required = false, label = "User")
    public static final String SECURITY_USERNAME = "security.usertoken.username";

    @SettingDefinition(type = Type.PASSWORD, order = 50, required = false, label = "Password")
    public static final String SECURITY_PASSWORD = "security.usertoken.password";

    HttpStreamable streamable;

    @Override
    protected void start(TypedProperties properties) {
        streamable = new HttpStreamable(properties.get(URL), properties.get(HTTP_METHOD,
                HTTP_METHOD_GET),
                properties.get(CONTENT_TYPE),
                properties.getInt(HTTP_TIMEOUT), properties.get(SECURITY),
                properties.get(SECURITY_USERNAME), properties.get(SECURITY_PASSWORD));
    }

    @Override
    public void stop() {
        streamable.close();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) streamable;
    }

}
