package org.jumpmind.metl.ui.common;

public abstract class AbstractSelectedValueMenuManager implements ISelectedValueMenuManager {

    String[] enabledPaths;
    
    @Override
    public boolean handle(String menuSelected, Object selected) {
        return false;
    }
    
    abstract protected String[] getEnabledPaths();

    @Override
    public boolean isEnabled(String menuSelected) {
        if (enabledPaths == null) {
            enabledPaths = getEnabledPaths();
        }
        
        if (enabledPaths != null) {
            for (String enabledPath : enabledPaths) {
                if (enabledPath.contains(menuSelected)) {
                    return true;
                }
            }
        }
        return false;
    }

}
