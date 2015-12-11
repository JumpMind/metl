package org.jumpmind.metl.ui.common;

import com.vaadin.data.fieldgroup.DefaultFieldGroupFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.TextField;

public class FieldFactory extends DefaultFieldGroupFieldFactory {

    private static final long serialVersionUID = 1L;
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public <T extends Field> T createField(Class<?> type, Class<T> fieldType) {
        Field<?> field =  super.createField(type, fieldType);
        massageField(field);
        return (T)field;
    }

    protected void massageField(Field<?> field) {
        if (field instanceof TextField) {
            ((TextField) field).setNullRepresentation("");
        }
    }

}