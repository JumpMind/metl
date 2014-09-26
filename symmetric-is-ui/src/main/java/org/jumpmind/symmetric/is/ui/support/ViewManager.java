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
                ViewLink menu = (ViewLink) view.getClass().getAnnotation(ViewLink.class);
                if (menu != null) {
                    if (isBlank(defaultView)) {
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

    public Map<Category, List<ViewLink>> getMenuItemsByCategory() {
        LinkedHashMap<Category, List<ViewLink>> menuItemsByCategory = new LinkedHashMap<Category, List<ViewLink>>();
        Category[] categories = Category.values();
        for (Category category : categories) {
            List<ViewLink> viewsByCategory = new ArrayList<ViewLink>();
            for (View view : views) {
                ViewLink menu = (ViewLink) view.getClass().getAnnotation(ViewLink.class);
                if (menu != null) {
                    if (menu.category() == category) {
                        viewsByCategory.add(menu);
                    }
                }
            }
            Collections.sort(viewsByCategory, new Comparator<ViewLink>() {
                @Override
                public int compare(ViewLink o1, ViewLink o2) {
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
