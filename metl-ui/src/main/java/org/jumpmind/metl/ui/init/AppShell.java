package org.jumpmind.metl.ui.init;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.AppShellSettings;
import com.vaadin.flow.theme.Theme;

@Push()
@PageTitle("Metl")
@Theme("apptheme")
public class AppShell implements AppShellConfigurator {
    private static final long serialVersionUID = 1L;

    @Override
    public void configurePage(AppShellSettings settings) {
        settings.addFavIcon("shortcut icon", "/metl/icons/favicon.ico", "32x32");
    }
}
