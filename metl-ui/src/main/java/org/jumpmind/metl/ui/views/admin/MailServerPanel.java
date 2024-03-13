/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.admin;

import jakarta.annotation.PostConstruct;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.MessagingException;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentStatus;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.core.runtime.resource.MailSession;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.UIScope;

@SuppressWarnings("serial")
@UiComponent
@UIScope
@Order(800)
@AdminMenuLink(name = "Mail Server", id = "Mail Server", icon = VaadinIcon.ENVELOPE_O)
public class MailServerPanel extends AbstractAdminPanel {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    boolean isChanged;

    public MailServerPanel() {
    }
    
    @PostConstruct
    @Override
    public void init() {
        final GlobalSetting hostNameSetting = getGlobalSetting(MailSession.SETTING_HOST_NAME, "localhost");
        final GlobalSetting transportSetting = getGlobalSetting(MailSession.SETTING_TRANSPORT, "smtp");
        final GlobalSetting portSetting = getGlobalSetting(MailSession.SETTING_PORT_NUMBER, "25");
        final GlobalSetting fromSetting = getGlobalSetting(MailSession.SETTING_FROM, "metl@localhost");
        final GlobalSetting usernameSetting = getGlobalSetting(MailSession.SETTING_USERNAME, "");
        final GlobalSetting passwordSetting = getGlobalSetting(MailSession.SETTING_PASSWORD, "");
        final GlobalSetting useTlsSetting = getGlobalSetting(MailSession.SETTING_USE_TLS, "false");
        final GlobalSetting useAuthSetting = getGlobalSetting(MailSession.SETTING_USE_AUTH, "false");

        setPadding(false);
        
        FormLayout form = new FormLayout();
        form.getStyle().set("padding-left", "16px");
        form.setResponsiveSteps(new ResponsiveStep("0", 1));

        TextField hostField = new TextField();
        hostField.setValueChangeMode(ValueChangeMode.LAZY);
        hostField.setValueChangeTimeout(200);
        hostField.addValueChangeListener(event -> saveSetting(hostNameSetting, event.getValue()));
        hostField.setValue(hostNameSetting.getValue());
        hostField.setWidth("25em");
        form.addFormItem(hostField, "Host name");
        hostField.focus();

        Select<String> transportField = new Select<String>();
        transportField.setItems("smtp", "smtps", "mock_smtp");
        transportField.setValue(transportSetting.getValue() == null ? "smtp" : transportSetting.getValue());
        transportField.setEmptySelectionAllowed(false);
        transportField.setWidth("10em");
        transportField.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
            public void valueChanged(ValueChangeEvent<String> event) {
                saveSetting(transportSetting, event.getValue());
            }
        });
        form.addFormItem(transportField, "Transport");

        TextField portField = new TextField();
        portField.setValueChangeMode(ValueChangeMode.LAZY);
        portField.setValueChangeTimeout(200);
        portField.addValueChangeListener(event -> saveSetting(portSetting, event.getValue()));
        portField.setValue(portSetting.getValue());
        portField.setWidth("25em");
        form.addFormItem(portField, "Port");

        TextField fromField = new TextField();
        fromField.setValueChangeMode(ValueChangeMode.LAZY);
        fromField.setValueChangeTimeout(200);
        fromField.addValueChangeListener(event -> saveSetting(fromSetting, event.getValue()));
        fromField.setValue(fromSetting.getValue());
        fromField.setWidth("25em");
        form.addFormItem(fromField, "From Address");

        Checkbox tlsField = new Checkbox("Use TLS", Boolean.valueOf(useTlsSetting.getValue()));
        tlsField.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<Boolean>>() {
            public void valueChanged(ValueChangeEvent<Boolean> event) {
                saveSetting(useTlsSetting, event.getValue().toString());
            }            
        });
        form.addFormItem(tlsField, "");

        TextField userField = new TextField();
        userField.setValueChangeMode(ValueChangeMode.LAZY);
        userField.setValueChangeTimeout(200);
        userField.addValueChangeListener(event -> saveSetting(usernameSetting, event.getValue()));
        userField.setValue(usernameSetting.getValue());
        userField.setWidth("25em");

        PasswordField passwordField = new PasswordField();
        passwordField.setValueChangeMode(ValueChangeMode.LAZY);
        passwordField.setValueChangeTimeout(200);
        passwordField.addValueChangeListener(event -> saveSetting(passwordSetting, event.getValue()));
        passwordField.setValue(passwordSetting.getValue());
        passwordField.setWidth("25em");

        Checkbox authField = new Checkbox("Use Authentication", Boolean.valueOf(useAuthSetting.getValue()));
        authField.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<Boolean>>() {
            public void valueChanged(ValueChangeEvent<Boolean> event) {
                Boolean isEnabled = event.getValue();
                saveSetting(useAuthSetting, isEnabled.toString());
                userField.setEnabled(isEnabled);
                passwordField.setEnabled(isEnabled);
            }            
        });
        userField.setEnabled(authField.getValue());
        passwordField.setEnabled(authField.getValue());
        form.addFormItem(authField, "");
        form.addFormItem(userField, "Username");
        form.addFormItem(passwordField, "Password");
        
        Button testButton = new Button("Test Connection");
        testButton.addClickListener(new TestClickListener());
        form.addFormItem(testButton, "");

        add(form);
    }

    private void saveSetting(GlobalSetting setting, String value) {
        setting.setValue(value);
        context.getConfigurationService().save(setting);
        isChanged = true;
    }

    @Override
    public boolean closing() {
        if (isChanged) {
            for (Agent agent : context.getOperationsService().findAgents()) {
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
        GlobalSetting setting = context.getOperationsService().findGlobalSetting(name);
        if (setting == null) {
            setting = new GlobalSetting();
            setting.setName(name);
            setting.setValue(defaultValue);
        }
        return setting;
    }

    class TestClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            MailSession mailSession = new MailSession(context.getOperationsService().findGlobalSettingsAsMap());
            try {                
                mailSession.getTransport();                
                CommonUiUtils.notify("SMTP Test", "Success!");
            } catch (AuthenticationFailedException e) {
                CommonUiUtils.notify("SMTP Test", "Failed with authentication exception: " + e.getMessage());
                log.warn("SMTP test failed authentication", e);
            } catch (MessagingException e) {
                CommonUiUtils.notify("SMTP Test", "Failed with message exception: " + e.getMessage());
                log.warn("SMTP test failed", e);
            } finally {
                mailSession.closeTransport();
            }
        }        
    }

    @Override
    protected void refresh() {
    }

}
