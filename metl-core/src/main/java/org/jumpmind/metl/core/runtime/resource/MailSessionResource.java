package org.jumpmind.metl.core.runtime.resource;

import java.util.HashMap;
import java.util.Map;

import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;
import org.jumpmind.metl.core.plugin.XMLSetting.Type;
import org.jumpmind.properties.TypedProperties;

@ResourceDefinition(typeName = MailSessionResource.TYPE, resourceCategory = ResourceCategory.MAIL_SESSION)
public class MailSessionResource extends AbstractResourceRuntime {

    public final static String TYPE = "MailSession";

    @SettingDefinition(type = Type.CHOICE, choices = { "smtp", "smtps",
            "mock_smtp" }, order = 10, defaultValue = "smtp", required = true, label = "Transport")
    public static final String SETTING_TRANSPORT = MailSession.SETTING_TRANSPORT;

    @SettingDefinition(order = 20, required = true, type = Type.TEXT, defaultValue = "localhost", label = "Hostname")
    public static final String SETTING_HOST_NAME = MailSession.SETTING_HOST_NAME;

    @SettingDefinition(order = 30, type = Type.INTEGER, defaultValue = "25", label = "Port Number")
    public static final String SETTING_PORT_NUMBER = MailSession.SETTING_PORT_NUMBER;

    @SettingDefinition(order = 40, type = Type.BOOLEAN, defaultValue = "false", label = "Use TLS")
    public static final String SETTING_USE_TLS = MailSession.SETTING_USE_TLS;

    @SettingDefinition(order = 50, type = Type.BOOLEAN, defaultValue = "false", label = "Use Auth")
    public static final String SETTING_USE_AUTH = MailSession.SETTING_USE_AUTH;

    @SettingDefinition(order = 60, required = true, type = Type.TEXT, label = "Username")
    public static final String SETTING_USERNAME = MailSession.SETTING_USERNAME;

    @SettingDefinition(order = 70, required = true, type = Type.PASSWORD, label = "Password")
    public static final String SETTING_PASSWORD = MailSession.SETTING_PASSWORD;

    @SettingDefinition(order = 80, required = true, type = Type.TEXT, label = "From Address")
    public static final String SETTING_FROM = MailSession.SETTING_FROM;

    Map<String, String> settings = new HashMap<String, String>();

    MailSession session;

    @Override
    public void stop() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T reference() {
        return (T) session;
    }

    @Override
    protected void start(TypedProperties properties) {
        for (Object obj : properties.keySet()) {
            settings.put((String) obj, properties.get((String) obj));
        }
        session = new MailSession(settings);
    }

}
