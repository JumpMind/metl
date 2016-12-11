package org.jumpmind.metl.ui.views.design.menu;

import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FolderName;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDependency;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.ui.common.AbstractMenuBar;
import org.jumpmind.metl.ui.views.design.DesignNavigator;

import com.vaadin.ui.AbstractSelect;

public class DesignMenuBar extends AbstractMenuBar {

    private static final long serialVersionUID = 1L;

    public DesignMenuBar(DesignNavigator navigator, AbstractSelect tree) {
        super(tree, new NothingSelectAction(navigator));
        addMenuManager(ProjectVersion.class, new ProjectVersionMenuManager(navigator));
        addMenuManager(Project.class, new ProjectMenuManager(navigator));
        addMenuManager(FlowName.class, new FlowMenuManager(navigator));
        addMenuManager(ResourceName.class, new ResourceMenuManager(navigator));
        addMenuManager(ModelName.class, new ModelMenuManager(navigator));
        addMenuManager(ProjectVersionDependency.class, new ProjectDependencyMenuManager(navigator));
        addMenuManager(FolderName.class, new FolderMenuManager(navigator));
    }

    @Override
    protected void buildMenu() {
        add("File|New|Project");
        add("File|New|Project Dependency");
        add("File|New|Flow|Design");
        add("File|New|Flow|Test");
        add("File|New|Model");
        add("File|New|Resource|Database");
        add("File|New|Resource|Directory|FTP");
        add("File|New|Resource|Directory|File System");
        add("File|New|Resource|Directory|JMS");
        add("File|New|Resource|Directory|SFTP");
        add("File|New|Resource|Directory|SMB");
        add("File|New|Resource|HTTP Resource");
        add("File|New|Resource|Mail Session");
        add("File|Open");
        addSeparator("File");
        add("File|Import...");        
        add("File|Export...");
        
        add("Edit|Rename");
        add("Edit|Copy");
        addSeparator("Edit");
        add("Edit|Remove");

    }

    static class NothingSelectAction extends AbstractDesignSelectedValueMenuManager {

        public NothingSelectAction(DesignNavigator navigator) {
            super(navigator);
        }
        
        @Override
        protected boolean isReadOnly(Object selected) {
            return false;
        }
        
    }

}
