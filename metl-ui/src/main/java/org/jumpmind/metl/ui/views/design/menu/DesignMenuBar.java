/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.design.menu;

import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FolderName;
import org.jumpmind.metl.core.model.ModelName;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.ui.common.AbstractMenuBar;
import org.jumpmind.metl.ui.i18n.MessageResource;
import org.jumpmind.metl.ui.i18n.MessageSource;
import org.jumpmind.metl.ui.views.design.DesignNavigator;
import org.postgresql.translation.messages_bg;

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
        addMenuManager(ProjectVersionDepends.class, new ProjectDependencyMenuManager(navigator));
        addMenuManager(FolderName.class, new FolderMenuManager(navigator));
    }

    @Override
    protected void buildMenu() {
        add( MessageResource.getNewProject());
        add( MessageResource.getNewBranch());
        add(MessageResource.getNewDependency());
        add(MessageResource.getNewDesign());
        add(MessageResource.getNewTest());
        add(MessageResource.getNewHierarchical());
        add(MessageResource.getNewRelational());
        add(MessageResource.getNewDatabase());
        add(MessageResource.getNewFTP());
        add(MessageResource.getNewFileSystem());
        add(MessageResource.getNewJMS());
        add(MessageResource.getNewSFTP());
        add(MessageResource.getNewSMB());
        add(MessageResource.getNewHTTP());
        add(MessageResource.getNewMailSession());
        add(MessageResource.getNewSubscribeJMS());
        add(MessageResource.getFileOpen());
        addSeparator(MessageSource.message("common.file"));
        add(MessageResource.getFileImport());        
        add(MessageResource.getFileExport());
        
        add(MessageResource.getEditRename());
        add(MessageResource.getEditCut());
        add(MessageResource.getEditCopy());
        add(MessageResource.getEditPaste());
        addSeparator(MessageSource.message("common.edit"));
        add(MessageResource.getEditChangeDependency());
        addSeparator(MessageSource.message("common.edit"));
        add(MessageResource.getEditRemove());
        
        add(MessageSource.message("common.tag"));
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
