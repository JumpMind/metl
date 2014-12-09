package org.jumpmind.symmetric.is.ui.support;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.ui.init.AppUI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.Navigator;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.ui.ComponentContainer;

@Component
@Scope(value="ui")
public class ViewManager implements Serializable {

    private static final long serialVersionUID = 1L;

    @Autowired(required = false)
    List<View> views;

    Navigator navigator;
    
    String defaultView;

    public ViewManager() {
    }

    public void init(AppUI ui, ComponentContainer container) {
        navigator = new Navigator(ui, container);
        navigator.setErrorView(new PageNotFoundView(this));
        if (views != null) {
            for (View view : views) {
                MenuLink menu = (MenuLink) view.getClass().getAnnotation(MenuLink.class);
                if (menu != null && menu.uiClass().equals(AppUI.class)) {
                    if (isBlank(defaultView) && menu.useAsDefault()) {
                        defaultView = menu.id();
                    }
                    navigator.addView(menu.id(), view);
                }
            }
        }
    }
    
    public void addViewChangeListener(ViewChangeListener listener) {
        this.navigator.addViewChangeListener(listener);
    }

    public Map<Category, List<MenuLink>> getMenuItemsByCategory() {
        LinkedHashMap<Category, List<MenuLink>> menuItemsByCategory = new LinkedHashMap<Category, List<MenuLink>>();
        Category[] categories = Category.values();
        for (Category category : categories) {
            List<MenuLink> viewsByCategory = new ArrayList<MenuLink>();
            for (View view : views) {
                MenuLink menu = (MenuLink) view.getClass().getAnnotation(MenuLink.class);
                if (menu != null) {
                    if (menu.category() == category) {
                        viewsByCategory.add(menu);
                    }
                }
            }
            Collections.sort(viewsByCategory, new Comparator<MenuLink>() {
                @Override
                public int compare(MenuLink o1, MenuLink o2) {
                    return new Integer(o1.menuOrder()).compareTo(new Integer(o2.menuOrder()));
                }
            });
            if (viewsByCategory.size() > 0) {
                menuItemsByCategory.put(category, viewsByCategory);
            }
        }
        return menuItemsByCategory;
    }

    public void navigateTo(String viewName) {
        navigator.navigateTo(viewName);
    }
    
    public void navigateToDefault() {
        navigateTo(defaultView);
    }

}
