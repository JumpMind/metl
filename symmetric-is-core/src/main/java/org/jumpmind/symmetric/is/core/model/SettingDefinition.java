package org.jumpmind.symmetric.is.core.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jumpmind.symmetric.is.core.runtime.component.definition.XMLSetting.Type;

@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface SettingDefinition {

    int order() default 0;
    
    Type type();

    boolean required() default false;

    String[] choices() default {};

    String defaultValue() default "";

    String label() default "";

    boolean visible() default true;

    /**
     * When set, this setting must be provided by the user/caller of the object
     * that defined the setting. For example, a file resource needs to be
     * provided the name of the file or an SMTP resource needs to be provided
     * the subject and to list for an email.
     */
    boolean provided() default false;

}
