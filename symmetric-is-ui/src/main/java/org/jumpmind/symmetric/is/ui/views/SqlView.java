package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.ui.init.SqlUI;
import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.MenuLink;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.context.annotation.Scope;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;

@UiComponent
@Scope("ui")
@MenuLink(id="sqlexplorer", category=Category.RUNTIME, menuOrder=20, uiClass=SqlUI.class, name = "Sql Explorer", icon=FontAwesome.DATABASE)
public class SqlView implements View {

    private static final long serialVersionUID = 1L;

    @Override
    public void enter(ViewChangeEvent event) {
    }

}
