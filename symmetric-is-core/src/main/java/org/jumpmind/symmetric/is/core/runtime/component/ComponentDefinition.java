package org.jumpmind.symmetric.is.core.runtime.component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.MessageType;
import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLComponent.ResourceCategory;

@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ComponentDefinition {

    String typeName();
    
    String iconImage() default "puzzle.png";
    
    ComponentCategory category();
    
    MessageType inputMessage() default MessageType.NONE;
    
    MessageType outgoingMessage() default MessageType.NONE;
    
    boolean inputOutputModelsMatch() default false;
    
    ResourceCategory resourceCategory() default ResourceCategory.NONE;
    
}
