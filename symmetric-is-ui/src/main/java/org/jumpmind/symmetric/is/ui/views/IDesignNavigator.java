package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.ui.views.design.PropertySheet;

public interface IDesignNavigator {
    
    public void setPropertySheet(PropertySheet propertySheet);
    
    public void refresh();
    
    public void select (Object item);
    
    public void open(Object item);

}
