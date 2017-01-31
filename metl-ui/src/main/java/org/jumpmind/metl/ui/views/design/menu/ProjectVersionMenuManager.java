package org.jumpmind.metl.ui.views.design.menu;

import org.apache.commons.lang.ArrayUtils;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.ui.common.CutCopyPasteManager;
import org.jumpmind.metl.ui.views.design.DesignNavigator;

public class ProjectVersionMenuManager extends AbstractDesignSelectedValueMenuManager {

    public ProjectVersionMenuManager(DesignNavigator navigator) {
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
    protected String[] getEnabledPaths(Object selected) {
        String[] enabledPaths = (String[]) ArrayUtils.addAll(super.getEnabledPaths(selected), new String[] {
                "File|New|Project Dependency",
                "File|New|Flow|Design",
                "File|New|Flow|Test",
                "File|New|Model",
                "File|New|Resource|Database",
                "File|New|Resource|Directory|FTP",
                "File|New|Resource|Directory|File System",
                "File|New|Resource|Directory|JMS",
                "File|New|Resource|Directory|SFTP",
                "File|New|Resource|Directory|SMB",
                "File|New|Resource|HTTP",
                "File|New|Resource|Mail Session",
                "File|Open",
                "File|Import...",        
                "File|Export...",
                "Edit|Rename",
                "Edit|Copy",
                "Edit|Remove",
        });
        if (navigator.getContext().getClipboard()
                .containsKey(CutCopyPasteManager.CLIPBOARD_OBJECT_TYPE)) {
            enabledPaths = (String[]) ArrayUtils.add(enabledPaths, "Edit|Paste");
        }
        ProjectVersion version = (ProjectVersion) selected;
        if (version.getVersionType() != null && (version.getVersionType().equalsIgnoreCase(ProjectVersion.VersionType.RELEASE.toString())
                || version.getVersionType().equalsIgnoreCase(ProjectVersion.VersionType.MASTER.toString()))) {
            enabledPaths = (String[]) ArrayUtils.add(enabledPaths, "File|New|Project Branch");
        }
        
        return enabledPaths;
    }
}
