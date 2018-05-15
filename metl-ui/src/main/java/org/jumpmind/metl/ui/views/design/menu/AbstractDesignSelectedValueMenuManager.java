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

import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.ui.common.AbstractSelectedValueMenuManager;
import org.jumpmind.metl.ui.views.design.DesignNavigator;

abstract public class AbstractDesignSelectedValueMenuManager extends AbstractSelectedValueMenuManager {

    protected DesignNavigator navigator;

    public AbstractDesignSelectedValueMenuManager(DesignNavigator navigator) {
        this.navigator = navigator;
    }
    
    @Override
    public boolean handle(String menuSelected, Object valueSelected) {
        boolean handled = false;
        if (!super.handle(menuSelected, valueSelected)) {
            if ("File|New|Project".equals(menuSelected)) {
                navigator.addNewProject();
                return true;
            } else if ("Edit|Rename".equals(menuSelected)) {
                navigator.startEditingItem((AbstractNamedObject) valueSelected);
                return true;
            } else if ("File|New|Project Dependency".equals(menuSelected)) {
                navigator.promptForNewDependency();
                return true;
            } else if ("File|New|Project Branch".equals(menuSelected)) {
                navigator.doNewProjectBranch();
                return true;
            } else if ("File|New|Flow|Design".equals(menuSelected)) {
                navigator.addNewFlow(false);
                return true;
            } else if ("File|New|Flow|Test".equals(menuSelected)) {
                navigator.addNewFlow(true);
                return true;
            } else if ("File|New|Model|Hierarchical".equals(menuSelected)) {
                navigator.addNewModel(Model.TYPE_HIERARCHICAL);
                return true;
            } else if ("File|New|Model|Relational".equals(menuSelected)) {
                navigator.addNewModel(Model.TYPE_RELATIONAL);
                return true;
            } else if ("File|New|Resource|Database".equals(menuSelected)) {
                navigator.addNewDatabase();
                return true;
            } else if ("File|New|Resource|Directory|FTP".equals(menuSelected)) {
                navigator.addNewFtpFileSystem();
                return true;
            } else if ("File|New|Resource|Subscribe|JMS".equals(menuSelected)) {
                navigator.addNewJmsSubscribe();
                return true;                
            } else if ("File|New|Resource|Directory|File System".equals(menuSelected)) {
                navigator.addNewLocalFileSystem();
                return true;
            } else if ("File|New|Resource|Directory|JMS".equals(menuSelected)) {
                navigator.addNewJMSFileSystem();
                return true;
            } else if ("File|New|Resource|Directory|SFTP".equals(menuSelected)) {
                navigator.addNewSftpFileSystem();
                return true;
            } else if ("File|New|Resource|Directory|SMB".equals(menuSelected)) {
                navigator.addNewSMBFileSystem();
                return true;
            } else if ("File|New|Resource|HTTP".equals(menuSelected)) {
                navigator.addNewHttpResource();
                return true;
            } else if ("File|New|Resource|Mail Session".equals(menuSelected)) {
                navigator.addNewMailSession();
                return true;
            } else if ("File|Import...".equals(menuSelected)) {
                navigator.doImport();
                return true;
            } else if ("File|Export...".equals(menuSelected)) {
                navigator.doExport();
                return true;
            } else if ("File|Open".equals(menuSelected)) {
                navigator.doOpen();
                return true;
            } else if ("Edit|Remove".equals(menuSelected)) {
                navigator.doRemove();
                return true;
            } else if ("Edit|Cut".equals(menuSelected)) {
                navigator.doCut();
                return true;
            } else if ("Edit|Copy".equals(menuSelected)) {
                navigator.doCopy();
                return true;
            } else if ("Edit|Paste".equals(menuSelected)) {
                navigator.doPaste();
            } else if ("Edit|Change Dependency Version".equals(menuSelected)) {
                navigator.doChangeDependencyVersion();
            } else if ("Tag".equals(menuSelected)) {
                navigator.doTag();
            }
        }
        return handled;
    }
    
    protected boolean isReadOnly(Object selected) {
        ProjectVersion projectVersion = navigator.findProjectVersion(selected);
        if (projectVersion != null) {
            return projectVersion.locked();
        } else {
            return false;
        }
    }
    
    protected String[] getDisabledPaths(Object selected) {
        if (isReadOnly(selected)) {
            return new String[] { "File|New|Project Dependency", "File|New|Flow|Design", "File|New|Flow|Test", 
                    "File|New|Model|Hierarchical", "File|New|Model|Relational",
                    "File|New|Resource|Database", "File|New|Resource|Directory|FTP", "File|New|Resource|Directory|File System",
                    "File|New|Resource|Directory|JMS", "File|New|Resource|Directory|SFTP", "File|New|Resource|Directory|SMB",
                    "File|New|Resource|HTTP", "File|New|Resource|Mail Session", "File|New|Resource|Subscribe|JMS", "Edit|Rename", "Tag" };
        } else {
            return null;
        }
    }    

    @Override
    protected String[] getEnabledPaths(Object selected) {        
        return new String[] { "File|New|Project", "View|Hidden", "File|Import..." };
    }

}
