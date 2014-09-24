package org.jumpmind.symmetric.is.ui.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.vaadin.server.FontAwesome;

@Target({java.lang.annotation.ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewLink {
    
    String id();
    
    String name();
    
    Category category() default Category.OTHER;
    
    int menuOrder() default 10;
    
    boolean useAsDefault() default false;
    
    FontAwesome icon() default FontAwesome.ASTERISK;

}
