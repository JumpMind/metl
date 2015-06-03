package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.ui.views.design.IComponentEditPanel;

public interface IUIFactory {

    public IComponentEditPanel create(String componentId);
    
    public void refresh();
    
}
