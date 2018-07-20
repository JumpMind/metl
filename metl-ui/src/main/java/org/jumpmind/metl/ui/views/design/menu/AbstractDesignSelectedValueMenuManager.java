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
import org.jumpmind.metl.ui.i18n.MessageResource;
import org.jumpmind.metl.ui.i18n.MessageSource;
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
            if (MessageResource.getNewProject().equals(menuSelected)) {
            String a =	MessageResource.getNewProject();
                navigator.addNewProject();
                return true;
            } else if (MessageResource.getEditRename().equals(menuSelected)) {
                navigator.startEditingItem((AbstractNamedObject) valueSelected);
                return true;
            } else if (MessageResource.getEditChangeDependency().equals(menuSelected)) {
                navigator.promptForNewDependency();
                return true;
            } else if (MessageResource.getNewBranch().equals(menuSelected)) {
                navigator.doNewProjectBranch();
                return true;
            } else if (MessageResource.getNewDesign().equals(menuSelected)) {
                navigator.addNewFlow(false);
                return true;
            } else if (MessageResource.getNewTest().equals(menuSelected)) {
                navigator.addNewFlow(true);
                return true;
            } else if (MessageResource.getNewHierarchical().equals(menuSelected)) {
                navigator.addNewModel(Model.TYPE_HIERARCHICAL);
                return true;
            } else if (MessageResource.getNewRelational().equals(menuSelected)) {
                navigator.addNewModel(Model.TYPE_RELATIONAL);
                return true;
            } else if (MessageResource.getNewDatabase().equals(menuSelected)) {
                navigator.addNewDatabase();
                return true;
            } else if (MessageResource.getNewFTP().equals(menuSelected)) {
                navigator.addNewFtpFileSystem();
                return true;
            } else if (MessageResource.getNewSubscribeJMS().equals(menuSelected)) {
                navigator.addNewJmsSubscribe();
                return true;                
            } else if (MessageResource.getNewFileSystem().equals(menuSelected)) {
                navigator.addNewLocalFileSystem();
                return true;
            } else if (MessageResource.getNewJMS().equals(menuSelected)) {
                navigator.addNewJMSFileSystem();
                return true;
            } else if (MessageResource.getNewSFTP().equals(menuSelected)) {
                navigator.addNewSftpFileSystem();
                return true;
            } else if (MessageResource.getNewSMB().equals(menuSelected)) {
                navigator.addNewSMBFileSystem();
                return true;
            } else if (MessageResource.getNewHTTP().equals(menuSelected)) {
                navigator.addNewHttpResource();
                return true;
            } else if (MessageResource.getNewMailSession().equals(menuSelected)) {
                navigator.addNewMailSession();
                return true;
            } else if (MessageResource.getFileImport().equals(menuSelected)) {
                navigator.doImport();
                return true;
            } else if (MessageResource.getFileExport().equals(menuSelected)) {
                navigator.doExport();
                return true;
            } else if (MessageResource.getFileOpen().equals(menuSelected)) {
                navigator.doOpen();
                return true;
            } else if (MessageResource.getEditRemove().equals(menuSelected)) {
                navigator.doRemove();
                return true;
            } else if (MessageResource.getEditCut().equals(menuSelected)) {
                navigator.doCut();
                return true;
            } else if (MessageResource.getEditCopy().equals(menuSelected)) {
                navigator.doCopy();
                return true;
            } else if (MessageResource.getEditPaste().equals(menuSelected)) {
                navigator.doPaste();
            } else if (MessageResource.getEditChangeDependency().equals(menuSelected)) {
                navigator.doChangeDependencyVersion();
            } else if (MessageSource.message("common.tag").equals(menuSelected)) {
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
            return new String[] { MessageResource.getNewDependency(), MessageResource.getNewDesign(), MessageResource.getNewTest(), 
            		MessageResource.getNewHierarchical(), MessageResource.getNewRelational(),
            		MessageResource.getNewDatabase(), MessageResource.getNewFTP(), MessageResource.getNewFileSystem(),
            		MessageResource.getNewJMS(), MessageResource.getNewSFTP(), MessageResource.getNewSMB(),
            		MessageResource.getNewHTTP(), MessageResource.getNewMailSession(), MessageResource.getNewSubscribeJMS(), MessageResource.getEditRename(), MessageSource.message("common.tag") };
        } else {
            return null;
        }
    }    

    @Override
    protected String[] getEnabledPaths(Object selected) {        
        return new String[] { MessageResource.getNewProject(), "View|Hidden", MessageResource.getFileImport() };
    }

}
