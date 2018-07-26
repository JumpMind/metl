package org.jumpmind.metl.ui.views.admin;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.ui.Component;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
@UiComponent
@Scope(value = "ui")
public abstract class AbstractAdminPanel extends VerticalLayout implements AdminSideView, IUiPanel {
   
    @Autowired
    ApplicationContext context;
 
    AdminView adminView;
    
    protected abstract void refresh();
    
    @PostConstruct 
    public void init() {
        refresh();
    }
    
    @Override
    public Component getView() {
        return this;
    }

    @Override
    public void setAdminView(AdminView view) {
        this.adminView = view;
    }
    
    public ApplicationContext getContext() {
        return this.context;
    }
    
    public AdminView getAdminView() {
        return this.adminView;
    }
    
    @Override
    public boolean isAccessible() {
        return true;
    }
}
