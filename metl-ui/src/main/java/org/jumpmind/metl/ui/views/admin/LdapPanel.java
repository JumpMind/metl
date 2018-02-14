package org.jumpmind.metl.ui.views.admin;

import org.jumpmind.metl.core.authentication.ConsoleAuthenticationConnectionException;
import org.jumpmind.metl.core.authentication.ConsoleAuthenticationCredentialException;
import org.jumpmind.metl.core.authentication.UserAuthenticationLDAP;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentStatus;
import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.Panel;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class LdapPanel extends Panel implements IUiPanel {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());
    
    ApplicationContext context;

    TabbedPanel tabbedPanel;
    
    boolean isChanged;
    
    ImmediateUpdateTextField hostField;
    ImmediateUpdateTextField baseDnField;
    ImmediateUpdateTextField searchAttrField;
    ImmediateUpdateTextField securityPrincipalField;

    public LdapPanel(final ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;

        final GlobalSetting hostNameSetting = getGlobalSetting(GlobalSetting.LDAP_HOST, "");
        final GlobalSetting baseDnSetting = getGlobalSetting(GlobalSetting.LDAP_BASE_DN, "");
        final GlobalSetting searchAttrSetting = getGlobalSetting(GlobalSetting.LDAP_SEARCH_ATR, "");
        final GlobalSetting securityPrincipalSetting = 
                getGlobalSetting(GlobalSetting.LDAP_SECURITY_PRINCIPAL, GlobalSetting.LDAP_SECURITY_PRINCIPAL_DEFAULT);
        
        FormLayout form = new FormLayout();
        form.setSpacing(true);

        hostField = new ImmediateUpdateTextField("LDAP URL") {
            private static final long serialVersionUID = 1L;

            protected void save(String value) {
                saveSetting(hostNameSetting, value);
            }
        };
        hostField.setDescription("e.g. ldap://hostname:389/");
        hostField.setValue(hostNameSetting.getValue());
        hostField.setWidth(25f, Unit.EM);
        form.addComponent(hostField);
        hostField.focus();

        baseDnField = new ImmediateUpdateTextField("Base DN") {
            private static final long serialVersionUID = 1L;

            protected void save(String value) {
                saveSetting(baseDnSetting, value);
            }
        };
        baseDnField.setDescription("e.g. dc=example,dc=com");
        baseDnField.setValue(baseDnSetting.getValue());
        baseDnField.setWidth(25f, Unit.EM);
        form.addComponent(baseDnField);
        
        searchAttrField = new ImmediateUpdateTextField("Search Attribute") {
            private static final long serialVersionUID = 1L;

            protected void save(String value) {
                saveSetting(searchAttrSetting, value);
            }
        };
        searchAttrField.setDescription("e.g. uid");
        searchAttrField.setValue(searchAttrSetting.getValue());
        searchAttrField.setWidth(25f, Unit.EM);
        form.addComponent(searchAttrField);
        
        securityPrincipalField = new ImmediateUpdateTextField("Security Principal") {
            private static final long serialVersionUID = 1L;

            protected void save(String value) {
                saveSetting(searchAttrSetting, value);
            }
        };
        securityPrincipalField.setDescription("default: " + GlobalSetting.LDAP_SECURITY_PRINCIPAL_DEFAULT 
                + " or just ${username} may work for Active Directory");
        securityPrincipalField.setValue(securityPrincipalSetting.getValue());
        securityPrincipalField.setWidth(25f, Unit.EM);
        form.addComponent(securityPrincipalField);
        
        Button testButton = new Button("Test");
        testButton.addClickListener(new TestClickListener());
        form.addComponent(testButton);
        
        VerticalLayout paddedLayout = new VerticalLayout();
        paddedLayout.setMargin(true);
        paddedLayout.addComponent(form);
        setContent(paddedLayout);
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

    class TestClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;

        public void buttonClick(ClickEvent event) {
            UI.getCurrent().addWindow( 
                    new LdapTestDialog());
        }
    }
    
    class LdapTestDialog extends ResizableWindow {
        private static final long serialVersionUID = 1L;
        
        private UserAuthenticationLDAP ldapAuthenticate;
        private TextField sampleUser;
        private PasswordField samplePassword;

        public LdapTestDialog() {
            super("LDAP Test");

            setModal(true);
            
            VerticalLayout layout = new VerticalLayout();
            content.addComponent(layout);
            
            layout.setSpacing(true);
            layout.setMargin(true);
            layout.setSizeFull();
            
            VerticalLayout scrollableLayout = new VerticalLayout();
            
            
            Panel scrollable = new Panel();
            scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);
            scrollable.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
            scrollable.setSizeFull();
            scrollable.setContent(scrollableLayout);
            layout.addComponent(scrollable);
            layout.setExpandRatio(scrollable, 1.0f);
            addComponent(layout, 1);
            setWidth(300, Unit.PIXELS);
            setHeight(200, Unit.PIXELS);
            setClosable(true);
            
            addComponentsToLayout(scrollableLayout);
            addButtonFooter();
            
            ldapAuthenticate = new UserAuthenticationLDAP();
        }
        
        protected void addButtonFooter() {
            Button testButton = new Button("Test");
            testButton.addClickListener(new Button.ClickListener() {
                private static final long serialVersionUID = 1L;
                public void buttonClick(ClickEvent event) {
                    testAuthentication();
                }
            });
            testButton.setClickShortcut(KeyCode.ENTER);
            
            Button cancelButton = new Button("Done");
            cancelButton.addClickListener(new ClickListener() {
                private static final long serialVersionUID = 1L;
                public void buttonClick(ClickEvent event) {
                    close();
                }
            });
            
            addComponent(buildButtonFooter(cancelButton, testButton));
        }

        protected void addComponentsToLayout(VerticalLayout fieldLayout) {
            sampleUser = new TextField("User Id");
            sampleUser.setNullRepresentation("");
            sampleUser.setValidationVisible(false);
            sampleUser.setWidth(100,Unit.PERCENTAGE);
            fieldLayout.addComponent(sampleUser);
            
            samplePassword = new PasswordField("Password");
            samplePassword.setNullRepresentation("");
            samplePassword.setValidationVisible(false);
            samplePassword.setWidth(100,Unit.PERCENTAGE);
            fieldLayout.addComponent(samplePassword);
        }
        
        private void testAuthentication() {
            try {
                if (ldapAuthenticate.authenticate(sampleUser.getValue(), samplePassword.getValue(), hostField.getValue(), baseDnField.getValue(), 
                        searchAttrField.getValue(), securityPrincipalField.getValue())) {
                    CommonUiUtils.notify("User authentication succeeded");
                }
            /* TODO fix these exceptions.  
             * Currently if the base DN is wrong, it says invalid user/password combo.
             * Currently if the host is wrong, it infinite loops
             * Currently if the search attribute is wrong, it says wrong DN
             */
            } catch (Throwable t) {
                log.warn("Failed LDAP test authentication", t);
                if (t instanceof ConsoleAuthenticationConnectionException) {                    
                    CommonUiUtils.notify("Unable to connect to network resource.");
                } else if (t instanceof ConsoleAuthenticationCredentialException) {
                    CommonUiUtils.notify("Invalid user id or password");                    
                } else {
                    CommonUiUtils.notify("General Error: " + t.getMessage(), Type.HUMANIZED_MESSAGE);                    
                }
            }
        }

    }

}
