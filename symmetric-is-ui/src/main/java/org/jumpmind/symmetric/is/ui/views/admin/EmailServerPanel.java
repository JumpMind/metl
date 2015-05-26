package org.jumpmind.symmetric.is.ui.views.admin;

import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EmailServerPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    public EmailServerPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;

        FormLayout form = new FormLayout();
        form.setSpacing(true);

        ImmediateUpdateTextField hostField = new ImmediateUpdateTextField("Host name") {
            protected void save(String value) {
            }            
        };
        hostField.setValue("myhost");
        hostField.setWidth(25f, Unit.EM);
        form.addComponent(hostField);
        hostField.focus();

        ImmediateUpdateTextField portField = new ImmediateUpdateTextField("SMTP Port") {
            protected void save(String value) {
            }            
        };
        portField.setValue("25");
        portField.setWidth(25f, Unit.EM);
        form.addComponent(portField);

        ImmediateUpdateTextField fromField = new ImmediateUpdateTextField("From Address") {
            protected void save(String value) {
            }            
        };
        fromField.setValue("sis@here.com");
        fromField.setWidth(25f, Unit.EM);
        form.addComponent(fromField);

        ImmediateUpdateTextField prefixField = new ImmediateUpdateTextField("Subject Prefix") {
            protected void save(String value) {
            }            
        };
        prefixField.setValue("SIS");
        prefixField.setWidth(25f, Unit.EM);
        form.addComponent(prefixField);

        ImmediateUpdateTextField userField = new ImmediateUpdateTextField("Username") {
            protected void save(String value) {
            }            
        };
        userField.setValue("myuser");
        userField.setWidth(25f, Unit.EM);
        form.addComponent(userField);

        ImmediateUpdateTextField passwordField = new ImmediateUpdateTextField("Password") {
            protected void save(String value) {
            }            
        };
        passwordField.setValue("mypassword");
        passwordField.setWidth(25f, Unit.EM);
        form.addComponent(passwordField);

        Button testButton = new Button("Test Connection");
        form.addComponent(testButton);

        addComponent(form);
        setMargin(true);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    class TestClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        }        
    }

}
