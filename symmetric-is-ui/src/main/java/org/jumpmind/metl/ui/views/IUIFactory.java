package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.ui.definition.XMLComponentUI;
import org.jumpmind.symmetric.is.ui.views.design.IComponentEditPanel;

public interface IUIFactory {
    
    public XMLComponentUI getDefinition(String componentId);

    public IComponentEditPanel create(String componentId);
    
    public void refresh();
    
}
