package org.jumpmind.metl.ui.views.design.menu;

import org.apache.commons.lang.ArrayUtils;
import org.jumpmind.metl.ui.views.design.DesignNavigator;

public class ProjectDependencyMenuManager extends AbstractDesignSelectedValueMenuManager {

    public ProjectDependencyMenuManager(DesignNavigator navigator) {
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
    protected String[] getDisabledPaths(Object selected) {
        if (isReadOnly(selected)) {
            return (String[])ArrayUtils.addAll(super.getDisabledPaths(selected), new String[] { "Edit|Remove",
                    "Edit|Change Dependency Version"
            });            
        } else {
            return (String[])ArrayUtils.addAll(super.getDisabledPaths(selected), new String[] {
                    "Edit|Copy"
            });
        }
    }
    
    @Override
    protected String[] getEnabledPaths(Object selected) {
        return (String[])ArrayUtils.addAll(super.getEnabledPaths(selected), new String[] {
                "File|New|Project Dependency",
                "File|New|Flow|Design",
                "File|New|Flow|Test",
                "File|New|Model|Hierarchical",
                "File|New|Model|Relational",
                "File|New|Resource|Database",
                "File|New|Resource|Directory|FTP",
                "File|New|Resource|Directory|File System",
                "File|New|Resource|Directory|JMS",
                "File|New|Resource|Directory|SFTP",
                "File|New|Resource|Directory|SMB",
                "File|New|Resource|HTTP",
                "File|New|Resource|Mail Session",
                "File|New|Resource|Subscribe|JMS",
                "Edit|Change Dependency Version",
                "Edit|Remove",
        });
    }
}
