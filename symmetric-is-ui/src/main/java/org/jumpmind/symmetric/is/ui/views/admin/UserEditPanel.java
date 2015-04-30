package org.jumpmind.symmetric.is.ui.views.admin;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.is.core.model.User;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")

public class UserEditPanel extends VerticalLayout implements IUiPanel {

    protected static final String NOCHANGE = "******";

    ApplicationContext context;
    
    User user;
    
    TextField loginField;
    
    TextField nameField;
    
    PasswordField passwordField; 
    
    public UserEditPanel(ApplicationContext context, User user) {
        this.context = context;
        this.user = user;
        
        FormLayout form = new FormLayout();
        form.setSpacing(true);

        loginField = new TextField("Login ID", StringUtils.trimToEmpty(user.getLoginId()));
        form.addComponent(loginField);
        loginField.focus();

        nameField = new TextField("Full Name", StringUtils.trimToEmpty(user.getName()));
        form.addComponent(nameField);

        passwordField = new PasswordField("Password", NOCHANGE);
        form.addComponent(passwordField);

        addComponent(form);
        setMargin(true);
    }
    
    @Override
    public boolean closing() {
        if (!loginField.getValue().equals(user.getLoginId()) || !nameField.getValue().equals(user.getName()) ||
                !passwordField.getValue().equals(NOCHANGE)) {
            user.setLoginId(loginField.getValue());
            user.setName(nameField.getValue());
            if (!passwordField.getValue().equals(NOCHANGE)) {
                user.setPassword(User.hashValue(passwordField.getValue()));   
            }
            context.getConfigurationService().save(user);
        }
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }
    
}
