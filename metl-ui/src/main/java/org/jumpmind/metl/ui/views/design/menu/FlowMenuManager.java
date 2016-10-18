package org.jumpmind.metl.ui.views.design.menu;

import org.apache.commons.lang.ArrayUtils;
import org.jumpmind.metl.ui.views.design.DesignNavigator;

public class FlowMenuManager extends AbstractDesignSelectedValueMenuManager {

    public FlowMenuManager(DesignNavigator navigator) {
        super(navigator);
    }
    
    @Override
    public boolean handle(String menuSelected, Object selected) {
        if (!super.handle(menuSelected, selected)) {            
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    protected String[] getEnabledPaths() {
        return (String[])ArrayUtils.addAll(super.getEnabledPaths(), new String[] {
                "File|New|Project Dependency",
                "File|New|Flow|Design",
                "File|New|Flow|Test",
                "File|New|Model",
                "File|New|Resource|Database",
                "File|New|Resource|Directory|FTP",
                "File|New|Resource|Directory|File System",
                "File|New|Resource|Directory|JMS Topic",
                "File|New|Resource|Directory|SFTP",
                "File|New|Resource|Directory|SMB",
                "File|New|Resource|Directory|HTTP Resource",
                "File|New|Resource|Mail Session",
                "File|Open",
                "File|Import...",        
                "File|Export...",
                "Edit|Rename",
                "Edit|Copy",
                "Edit|Remove"
        });
    }
}
