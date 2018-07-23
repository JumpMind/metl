package org.jumpmind.metl.ui.views.admin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jumpmind.metl.ui.init.AppUI;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.UI;

@Target({java.lang.annotation.ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AdminMenuLink {
    String id();
    
    String name();
    
    Class<? extends UI> uiClass() default AppUI.class;
    
    FontAwesome icon() default FontAwesome.ASTERISK;
}
