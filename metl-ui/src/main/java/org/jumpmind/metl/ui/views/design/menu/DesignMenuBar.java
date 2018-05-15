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
        addMenuManager(ProjectVersionDepends.class, new ProjectDependencyMenuManager(navigator));
        addMenuManager(FolderName.class, new FolderMenuManager(navigator));
    }

    @Override
    protected void buildMenu() {
        add("File|New|Project");
        add("File|New|Project Branch");
        add("File|New|Project Dependency");
        add("File|New|Flow|Design");
        add("File|New|Flow|Test");
        add("File|New|Model|Hierarchical");
        add("File|New|Model|Relational");
        add("File|New|Resource|Database");
        add("File|New|Resource|Directory|FTP");
        add("File|New|Resource|Directory|File System");
        add("File|New|Resource|Directory|JMS");
        add("File|New|Resource|Directory|SFTP");
        add("File|New|Resource|Directory|SMB");
        add("File|New|Resource|HTTP");
        add("File|New|Resource|Mail Session");
        add("File|New|Resource|Subscribe|JMS");
        add("File|Open");
        addSeparator("File");
        add("File|Import...");        
        add("File|Export...");
        
        add("Edit|Rename");
        add("Edit|Cut");
        add("Edit|Copy");
        add("Edit|Paste");
        addSeparator("Edit");
        add("Edit|Change Dependency Version");
        addSeparator("Edit");
        add("Edit|Remove");
        
        add("Tag");
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
