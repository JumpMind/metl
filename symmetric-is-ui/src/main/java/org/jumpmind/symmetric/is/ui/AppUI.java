package org.jumpmind.symmetric.is.ui;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Responsive;
import com.vaadin.server.ThemeResource;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;

@Theme("apptheme")
@Title("SymmetricIS")
@PreserveOnRefresh
@Push(transport = Transport.STREAMING)
public class AppUI extends UI {

    private static final long serialVersionUID = 1L;

    CssLayout contentArea = new CssLayout();

    CssLayout menuArea = new CssLayout();

    @Override
    protected void init(VaadinRequest request) {

        HorizontalLayout root = new HorizontalLayout();
        root.setSizeFull();
        setContent(root);

        Responsive.makeResponsive(this);

        menuArea.setPrimaryStyleName("valo-menu");

        contentArea.setPrimaryStyleName("valo-content");
        contentArea.addStyleName("v-scrollable");
        contentArea.setSizeFull();

        root.addComponents(menuArea, contentArea);
        root.setExpandRatio(contentArea, 1);

        final CssLayout menu = new CssLayout();
        CssLayout menuItemsLayout = new CssLayout();
        {
            menu.setId("testMenu");
        }

        menu.addStyleName("valo-menu-part");

        final HorizontalLayout top = new HorizontalLayout();
        top.setWidth("100%");
        top.setDefaultComponentAlignment(Alignment.MIDDLE_LEFT);
        top.addStyleName("valo-menu-title");
        menu.addComponent(top);

        final Button showMenu = new Button("Menu", new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                if (menu.getStyleName().contains("valo-menu-visible")) {
                    menu.removeStyleName("valo-menu-visible");
                } else {
                    menu.addStyleName("valo-menu-visible");
                }
            }
        });
        showMenu.addStyleName(ValoTheme.BUTTON_PRIMARY);
        showMenu.addStyleName(ValoTheme.BUTTON_SMALL);
        showMenu.addStyleName("valo-menu-toggle");
        showMenu.setIcon(FontAwesome.LIST);
        menu.addComponent(showMenu);

        final Label title = new Label("<h3>JumpMind <strong>SymmetricIS</strong></h3>",
                ContentMode.HTML);
        title.setSizeUndefined();
        top.addComponent(title);
        top.setExpandRatio(title, 1);

        final MenuBar settings = new MenuBar();
        settings.addStyleName("user-menu");
        final MenuItem settingsItem = settings.addItem("nouser", new ThemeResource(
                "../images/profile-pic-300px.jpg"), null);
        settingsItem.addItem("Edit Profile", null);
        settingsItem.addItem("Preferences", null);
        settingsItem.addSeparator();
        settingsItem.addItem("Sign Out", null);
        menu.addComponent(settings);

        menuItemsLayout.setPrimaryStyleName("valo-menuitems");
        menu.addComponent(menuItemsLayout);

        addMenuSection("Manage", menuItemsLayout);
        addMenuItem("Agents", FontAwesome.GEARS, menuItemsLayout, "agents");
        // addMenuItem("Sql Explorer", FontAwesome.DATABASE, menuItemsLayout,
        // "agents");

        addMenuSection("Configure", menuItemsLayout);
        addMenuItem("Connections", FontAwesome.LINK, menuItemsLayout, "connections");
        addMenuItem("Models", FontAwesome.SITEMAP, menuItemsLayout, "models");
        addMenuItem("Components", FontAwesome.PUZZLE_PIECE, menuItemsLayout, "components");
        addMenuItem("Integrations", FontAwesome.SHARE, menuItemsLayout, "integrations");

        menuArea.addComponent(menu);

    }

    protected void addMenuSection(String caption, CssLayout menuItemsLayout) {
        Label label = new Label(caption, ContentMode.HTML);
        label.setPrimaryStyleName("valo-menu-subtitle");
        label.addStyleName("h4");
        label.setSizeUndefined();
        menuItemsLayout.addComponent(label);
    }

    protected void addMenuItem(String caption, FontAwesome icon, CssLayout menuItemsLayout,
            String viewName) {
        final Button b = new Button(caption, new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                // navigator.navigateTo(item.getKey());
            }
        });

        b.setHtmlContentAllowed(true);
        b.setPrimaryStyleName("valo-menu-item");
        b.setIcon(icon);
        menuItemsLayout.addComponent(b);
    }

    @WebServlet(value = "/*", asyncSupported = true)
    @VaadinServletConfiguration(productionMode = false, ui = AppUI.class, widgetset = "org.jumpmind.symmetric.is.ui.AppWidgetSet")
    public static class Servlet extends VaadinServlet {

        private static final long serialVersionUID = 1L;

        @Override
        protected void servletInitialized() throws ServletException {
            super.servletInitialized();
            getService().addSessionInitListener(new AppSessionInitListener());
        }
    }

}
