package org.jumpmind.symmetric.is.core.runtime.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionCategory;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentDefinition {

    String typeName();
    
    ComponentCategory category();
    
    ComponentSupports[] supports();
    
    ConnectionCategory connectionCategory() default ConnectionCategory.NONE;
    
}
