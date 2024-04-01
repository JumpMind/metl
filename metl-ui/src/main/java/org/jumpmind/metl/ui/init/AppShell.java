package org.jumpmind.metl.ui.init;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.Theme;

@Push()
@PageTitle("Metl")
@Theme("apptheme")
public class AppShell implements AppShellConfigurator {
    private static final long serialVersionUID = 1L;

}
