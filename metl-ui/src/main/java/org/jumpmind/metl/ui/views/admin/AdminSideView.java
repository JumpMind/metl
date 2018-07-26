package org.jumpmind.metl.ui.views.admin;

import com.vaadin.navigator.View;
import com.vaadin.ui.Component;

public interface AdminSideView extends View {

    Component getView();
    
    void setAdminView(AdminView view);
    
    boolean isAccessible();
}
