package org.jumpmind.symmetric.is.core.runtime.resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceDefinition {

    String typeName();
    
    ResourceCategory resourceCategory();
    
}
