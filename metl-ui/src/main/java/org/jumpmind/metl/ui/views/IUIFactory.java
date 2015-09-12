package org.jumpmind.metl.ui.views;

import org.jumpmind.metl.ui.definition.XMLComponentUI;
import org.jumpmind.metl.ui.views.design.IComponentEditPanel;

public interface IUIFactory {
    
    public XMLComponentUI getDefinition(String componentId);

    public IComponentEditPanel create(String componentId);
    
    public void refresh();
    
}
