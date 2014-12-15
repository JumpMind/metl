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
                TopBarLink menu = (TopBarLink) view.getClass().getAnnotation(TopBarLink.class);
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

    public Map<Category, List<TopBarLink>> getMenuItemsByCategory() {
        LinkedHashMap<Category, List<TopBarLink>> menuItemsByCategory = new LinkedHashMap<Category, List<TopBarLink>>();
        Category[] categories = Category.values();
        for (Category category : categories) {
            List<TopBarLink> viewsByCategory = new ArrayList<TopBarLink>();
            for (View view : views) {
                TopBarLink menu = (TopBarLink) view.getClass().getAnnotation(TopBarLink.class);
                if (menu != null) {
                    if (menu.category() == category) {
                        viewsByCategory.add(menu);
                    }
                }
            }
            Collections.sort(viewsByCategory, new Comparator<TopBarLink>() {
                @Override
                public int compare(TopBarLink o1, TopBarLink o2) {
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
    
    public String getDefaultView() {
        return defaultView;
    }

}
