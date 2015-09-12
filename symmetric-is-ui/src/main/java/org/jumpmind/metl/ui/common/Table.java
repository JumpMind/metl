package org.jumpmind.symmetric.is.ui.common;

import java.util.Date;

import org.jumpmind.symmetric.ui.common.CommonUiUtils;

import com.vaadin.data.Property;

public class Table extends com.vaadin.ui.Table {

    private static final long serialVersionUID = 1L;
    @Override
    protected String formatPropertyValue(Object rowId, Object colId, Property<?> property) {
        if (property.getValue() != null) {
            if (property.getType() == Date.class) {
                return CommonUiUtils.formatDateTime((Date) property.getValue());
            }
        }
        return super.formatPropertyValue(rowId, colId, property);
    }

}
