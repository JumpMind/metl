package org.jumpmind.metl.ui.views.design.menu;

import org.apache.commons.lang.ArrayUtils;
import org.jumpmind.metl.ui.views.design.DesignNavigator;

public class ProjectMenuManager extends AbstractDesignSelectedValueMenuManager {

    public ProjectMenuManager(DesignNavigator navigator) {
        super(navigator);
    }
    
    @Override
    protected String[] getEnabledPaths(Object selected) {
        return (String[])ArrayUtils.addAll(super.getEnabledPaths(selected), new String[] {
                "File|Hide",
                "Edit|Rename",
                "Edit|Remove",
        });
    }

}
