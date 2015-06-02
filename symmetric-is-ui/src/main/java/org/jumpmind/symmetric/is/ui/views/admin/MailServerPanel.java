package org.jumpmind.symmetric.is.ui.views.admin;

import javax.mail.AuthenticationFailedException;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentStatus;
import org.jumpmind.symmetric.is.core.model.GlobalSetting;
import org.jumpmind.symmetric.is.core.model.MailServer;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ImmediateUpdatePasswordField;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.NativeSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class MailServerPanel extends VerticalLayout implements IUiPanel {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    ApplicationContext context;

    TabbedPanel tabbedPanel;
    
    boolean isChanged;

    public MailServerPanel(final ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;

        final GlobalSetting hostNameSetting = getGlobalSetting(MailServer.SETTING_HOST_NAME, "localhost");
        final GlobalSetting transportSetting = getGlobalSetting(MailServer.SETTING_TRANSPORT, "smtp");
        final GlobalSetting portSetting = getGlobalSetting(MailServer.SETTING_PORT_NUMBER, "25");
        final GlobalSetting fromSetting = getGlobalSetting(MailServer.SETTING_FROM, "symmetricis@localhost");
        final GlobalSetting usernameSetting = getGlobalSetting(MailServer.SETTING_USERNAME, "");
        final GlobalSetting passwordSetting = getGlobalSetting(MailServer.SETTING_PASSWORD, "");
        final GlobalSetting useTlsSetting = getGlobalSetting(MailServer.SETTING_USE_TLS, "false");
        final GlobalSetting useAuthSetting = getGlobalSetting(MailServer.SETTING_USE_AUTH, "false");

        FormLayout form = new FormLayout();
        form.setSpacing(true);

        ImmediateUpdateTextField hostField = new ImmediateUpdateTextField("Host name") {
            protected void save(String value) {
                saveSetting(hostNameSetting, value);
            }
        };
        hostField.setValue(hostNameSetting.getValue());
        hostField.setWidth(25f, Unit.EM);
        form.addComponent(hostField);
        hostField.focus();

        NativeSelect transportField = new NativeSelect("Transport");
        transportField.addItem("smtp");
        transportField.addItem("smtps");
        transportField.select(transportSetting.getValue() == null ? "smtp" : transportSetting.getValue());
        transportField.setNullSelectionAllowed(false);
        transportField.setImmediate(true);
        transportField.setWidth(10f, Unit.EM);
        transportField.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                saveSetting(transportSetting, (String) event.getProperty().getValue());
            }
        });
        form.addComponent(transportField);

        ImmediateUpdateTextField portField = new ImmediateUpdateTextField("Port") {
            protected void save(String value) {
                saveSetting(portSetting, value);
            }
        };
        portField.setValue(portSetting.getValue());
        portField.setWidth(25f, Unit.EM);
        form.addComponent(portField);

        ImmediateUpdateTextField fromField = new ImmediateUpdateTextField("From Address") {
            protected void save(String value) {
                saveSetting(fromSetting, value);
            }
        };
        fromField.setValue(fromSetting.getValue());
        fromField.setWidth(25f, Unit.EM);
        form.addComponent(fromField);

        CheckBox tlsField = new CheckBox("Use TLS", Boolean.valueOf(useTlsSetting.getValue()));
        tlsField.setImmediate(true);
        tlsField.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                saveSetting(useTlsSetting, ((Boolean) event.getProperty().getValue()).toString());
            }            
        });
        form.addComponent(tlsField);

        final ImmediateUpdateTextField userField = new ImmediateUpdateTextField("Username") {
            protected void save(String value) {
                saveSetting(usernameSetting, value);
            }            
        };
        userField.setValue(usernameSetting.getValue());
        userField.setWidth(25f, Unit.EM);

        final ImmediateUpdatePasswordField passwordField = new ImmediateUpdatePasswordField("Password") {
            protected void save(String value) {
                saveSetting(passwordSetting, value);
            }            
        };
        passwordField.setValue(passwordSetting.getValue());
        passwordField.setWidth(25f, Unit.EM);

        CheckBox authField = new CheckBox("Use Authentication", Boolean.valueOf(useAuthSetting.getValue()));
        authField.setImmediate(true);
        authField.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                Boolean isEnabled = (Boolean) event.getProperty().getValue();
                saveSetting(useAuthSetting, isEnabled.toString());
                userField.setEnabled(isEnabled);
                passwordField.setEnabled(isEnabled);
            }            
        });
        form.addComponent(authField);
        userField.setEnabled(authField.getValue());
        form.addComponent(userField);
        passwordField.setEnabled(authField.getValue());
        form.addComponent(passwordField);
        
        Button testButton = new Button("Test Connection");
        testButton.addClickListener(new TestClickListener());
        form.addComponent(testButton);

        addComponent(form);
        setMargin(true);
    }

    private void saveSetting(GlobalSetting setting, String value) {
        setting.setValue(value);
        context.getConfigurationService().save(setting);
        isChanged = true;
    }

    @Override
    public boolean closing() {
        if (isChanged) {
            for (Agent agent : context.getConfigurationService().findAgents()) {
                if (!agent.isDeleted() && agent.getStatus().equals(AgentStatus.RUNNING.name())) {
                    agent.setStatus(AgentStatus.REQUEST_REFRESH.name());
                    context.getConfigurationService().save(agent);
                }
            }
        }
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    private GlobalSetting getGlobalSetting(String name, String defaultValue) {
        GlobalSetting setting = context.getConfigurationService().findGlobalSetting(name);
        if (setting == null) {
            setting = new GlobalSetting();
            setting.setName(name);
            setting.setValue(defaultValue);
        }
        return setting;
    }

    class TestClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            try {
                MailServer mailServer = context.getConfigurationService().findMailServer();
                Session session = Session.getInstance(mailServer.getProperties());
                Transport transport = session.getTransport(mailServer.getTransport());
                if (mailServer.isUseAuth()) {
                    transport.connect(mailServer.getUsername(), mailServer.getPassword());
                } else {
                    transport.connect();
                }
                transport.close();
                CommonUiUtils.notify("SMTP Test", "Success!");
            } catch (AuthenticationFailedException e) {
                CommonUiUtils.notify("SMTP Test", "Failed with authentication exception: " + e.getMessage());
                log.warn("SMTP test failed authentication", e);
            } catch (MessagingException e) {
                CommonUiUtils.notify("SMTP Test", "Failed with message exception: " + e.getMessage());
                log.warn("SMTP test failed", e);
            }
        }        
    }

}
