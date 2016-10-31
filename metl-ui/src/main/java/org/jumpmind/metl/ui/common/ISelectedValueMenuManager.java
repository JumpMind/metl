package org.jumpmind.metl.ui.common;

public interface ISelectedValueMenuManager {

    public boolean handle(String menuSelected, Object selected);
    
    public boolean isEnabled(String menuSelected);
    
}
