package org.jumpmind.metl.ui.common;

public abstract class AbstractSelectedValueMenuManager implements ISelectedValueMenuManager {

    @Override
    public boolean handle(String menuSelected, Object selected) {
        return false;
    }

    abstract protected String[] getEnabledPaths(Object selected);

    abstract protected String[] getDisabledPaths(Object selected);

    @Override
    public boolean isEnabled(String menuSelected, Object selected) {
        String[] enabledPaths = getEnabledPaths(selected);
        if (enabledPaths != null) {
            String[] disabledPaths = getDisabledPaths(selected);
            if (disabledPaths != null) {
                for (String disabledPath : disabledPaths) {
                    if (disabledPath.equals(menuSelected)) {
                        return false;
                    }
                }
            }

            for (String enabledPath : enabledPaths) {
                if (enabledPath.contains(menuSelected)) {
                    return true;
                }
            }
        }
        return false;
    }

}
