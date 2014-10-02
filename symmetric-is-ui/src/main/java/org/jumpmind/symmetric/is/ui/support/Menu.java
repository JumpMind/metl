package org.jumpmind.symmetric.is.ui.support;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.themes.ValoTheme;

public class Menu extends CssLayout implements ViewChangeListener {

    private static final long serialVersionUID = 1L;

    CssLayout menu;

    CssLayout menuItemsLayout;

    ViewManager viewManager;

    Map<String, Button> viewToButtonMapping;

    public Menu(ViewManager viewManager) {
        setPrimaryStyleName("valo-menu");

        this.viewManager = viewManager;
        this.viewManager.addViewChangeListener(this);

        viewToButtonMapping = new HashMap<String, Button>();

        menu = new CssLayout();
        menuItemsLayout = new CssLayout();

        menu.addStyleName("valo-menu-part");

        HorizontalLayout top = new HorizontalLayout();
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

        Label title = new Label("<h3>JumpMind's <strong>SymmetricIS</strong></h3>", ContentMode.HTML);
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

        Map<Category, List<ViewLink>> menuItemsByCategory = viewManager.getMenuItemsByCategory();
        Set<Category> categories = menuItemsByCategory.keySet();
        for (Category category : categories) {
            addMenuSection(category.name(), menuItemsLayout);
            List<ViewLink> items = menuItemsByCategory.get(category);
            for (ViewLink menuView : items) {
                addMenuItem(menuView.name(), menuView.icon(), menuItemsLayout, menuView.id());
            }
        }

        addComponent(menu);
    }

    protected void addMenuSection(String caption, CssLayout menuItemsLayout) {
        Label label = new Label(caption, ContentMode.HTML);
        label.setPrimaryStyleName("valo-menu-subtitle");
        label.addStyleName("h4");
        label.setSizeUndefined();
        menuItemsLayout.addComponent(label);
    }

    protected void addMenuItem(String caption, FontAwesome icon, CssLayout menuItemsLayout,
            final String viewName) {

        final Button b = new Button(caption, new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                viewManager.navigateTo(viewName);
                event.getButton().addStyleName("selected");
            }
        });

        b.setHtmlContentAllowed(true);
        b.setPrimaryStyleName("valo-menu-item");
        b.setIcon(icon);
        menuItemsLayout.addComponent(b);

        viewToButtonMapping.put(viewName, b);
    }

    @Override
    public boolean beforeViewChange(final ViewChangeEvent event) {
        return true;
    }

    @Override
    public void afterViewChange(final ViewChangeEvent event) {
        for (final Iterator<Component> it = menuItemsLayout.iterator(); it.hasNext();) {
            it.next().removeStyleName("selected");
        }
        menu.removeStyleName("valo-menu-visible");
        Button button = viewToButtonMapping.get(event.getViewName());
        if (button != null) {
            button.setStyleName("selected");
        }
    }

}
