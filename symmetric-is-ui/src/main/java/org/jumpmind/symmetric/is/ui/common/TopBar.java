package org.jumpmind.symmetric.is.ui.common;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

public class TopBar extends HorizontalLayout implements ViewChangeListener {

    private static final long serialVersionUID = 1L;

    MenuBar menuBar;

    ViewManager viewManager;

    Map<String, MenuItem> viewToButtonMapping;

    List<MenuItem> categoryItems = new ArrayList<MenuBar.MenuItem>();

    DesignAgentSelect designAgentSelect;

    public TopBar(ViewManager vm, DesignAgentSelect das) {
        setWidth(100, Unit.PERCENTAGE);
        setSpacing(true);
        setMargin(new MarginInfo(true, true, false, true));

        this.designAgentSelect = das;
        this.viewManager = vm;
        this.viewManager.addViewChangeListener(this);

        viewToButtonMapping = new HashMap<String, MenuItem>();

        menuBar = new MenuBar();
        addComponent(menuBar);
        setComponentAlignment(menuBar, Alignment.MIDDLE_LEFT);
        
        addComponent(designAgentSelect);
        setComponentAlignment(designAgentSelect, Alignment.MIDDLE_RIGHT);

        Map<Category, List<TopBarLink>> menuItemsByCategory = viewManager.getMenuItemsByCategory();
        Set<Category> categories = menuItemsByCategory.keySet();
        for (Category category : categories) {
            List<TopBarLink> links = menuItemsByCategory.get(category);
            MenuItem categoryItem = null;
            if (links.size() > 1) {
                categoryItem = menuBar.addItem(category.name(), null);
                categoryItem.setCheckable(true);
                categoryItems.add(categoryItem);
            }
            for (final TopBarLink menuLink : links) {
                Command command = new Command() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void menuSelected(MenuItem selectedItem) {
                        uncheckAll();
                        selectedItem.setChecked(true);
                        viewManager.navigateTo(menuLink.id());
                    }
                };
                MenuItem menuItem = null;
                if (categoryItem == null) {
                    menuItem = menuBar.addItem(menuLink.name(), command);
                } else {
                    menuItem = categoryItem.addItem(menuLink.name(), command);
                }
                menuItem.setCheckable(true);
                viewToButtonMapping.put(menuLink.id(), menuItem);
            }
        }

    }

    protected void uncheckAll() {
        for (MenuItem menuItem : categoryItems) {
            menuItem.setChecked(false);
        }
        for (MenuItem menuItem : viewToButtonMapping.values()) {
            menuItem.setChecked(false);
        }
    }

    @Override
    public boolean beforeViewChange(final ViewChangeEvent event) {
        return true;
    }

    @Override
    public void afterViewChange(final ViewChangeEvent event) {
        String view = event.getViewName();
        if (isBlank(view)) {
            view = viewManager.getDefaultView();
        }
        MenuItem menuItem = viewToButtonMapping.get(view);
        if (menuItem != null) {
            uncheckAll();
            menuItem.setChecked(true);
        }
        designAgentSelect.refresh();
    }

}
