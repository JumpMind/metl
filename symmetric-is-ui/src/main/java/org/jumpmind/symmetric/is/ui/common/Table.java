package org.jumpmind.symmetric.is.ui.common;

import java.util.Date;

import org.apache.commons.lang.time.FastDateFormat;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;

import com.vaadin.data.Property;

public class Table extends com.vaadin.ui.Table {

    private static final long serialVersionUID = 1L;

    static final FastDateFormat DATETIMEFORMAT = FastDateFormat.getDateTimeInstance(
            FastDateFormat.SHORT, FastDateFormat.SHORT);

    static final FastDateFormat TIMEFORMAT = FastDateFormat.getTimeInstance(FastDateFormat.MEDIUM);

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
