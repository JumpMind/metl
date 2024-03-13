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
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.ui.common.AbstractSelectedValueMenuManager;
import org.jumpmind.metl.ui.views.design.DesignNavigator;

abstract public class AbstractDesignSelectedValueMenuManager extends AbstractSelectedValueMenuManager {

    protected DesignNavigator navigator;

    public AbstractDesignSelectedValueMenuManager(DesignNavigator navigator) {
        this.navigator = navigator;
    }
    
    @Override
    public boolean handle(String menuSelected, AbstractNamedObject valueSelected) {
        boolean handled = false;
        if (!super.handle(menuSelected, valueSelected)) {
            if ("File|New|Project".equals(menuSelected)) {
                navigator.addNewProject();
                return true;
            } else if ("Edit|Rename".equals(menuSelected)) {
                navigator.startEditingItem(valueSelected);
                return true;
            } else if ("File|New|Project Dependency".equals(menuSelected)) {
                navigator.promptForNewDependency(valueSelected);
                return true;
            } else if ("File|New|Project Branch".equals(menuSelected)) {
                navigator.doNewProjectBranch(valueSelected);
                return true;
            } else if ("File|New|Flow|Design".equals(menuSelected)) {
                navigator.addNewFlow(valueSelected, false);
                return true;
            } else if ("File|New|Flow|Test".equals(menuSelected)) {
                navigator.addNewFlow(valueSelected, true);
                return true;
            } else if ("File|New|Model|Hierarchical".equals(menuSelected)) {
                navigator.addNewHierarhicalModel(valueSelected);
                return true;
            } else if ("File|New|Model|Relational".equals(menuSelected)) {
                navigator.addNewRelationalModel(valueSelected);
                return true;
            } else if ("File|New|Resource|Database".equals(menuSelected)) {
                navigator.addNewDatabase(valueSelected);
                return true;
            } else if ("File|New|Resource|Directory|FTP".equals(menuSelected)) {
                navigator.addNewFtpFileSystem(valueSelected);
                return true;
            } else if ("File|New|Resource|Subscribe|JMS".equals(menuSelected)) {
                navigator.addNewJmsSubscribe(valueSelected);
                return true;          
            } else if ("File|New|Resource|Queue|Kafka Publisher".equals(menuSelected)) {
                navigator.addNewKafkaProducer(valueSelected);
                return true;                          
            } else if ("File|New|Resource|Queue|SQS".equals(menuSelected)) {
                navigator.addNewSqsQueue(valueSelected);
                return true;                          
            } else if ("File|New|Resource|Directory|File System".equals(menuSelected)) {
                navigator.addNewLocalFileSystem(valueSelected);
                return true;
            } else if ("File|New|Resource|Directory|JMS".equals(menuSelected)) {
                navigator.addNewJMSFileSystem(valueSelected);
                return true;
            } else if ("File|New|Resource|Directory|SFTP".equals(menuSelected)) {
                navigator.addNewSftpFileSystem(valueSelected);
                return true;
            } else if ("File|New|Resource|Directory|SMB".equals(menuSelected)) {
                navigator.addNewSMBFileSystem(valueSelected);
                return true;
            } else if ("File|New|Resource|HTTP".equals(menuSelected)) {
                navigator.addNewHttpResource(valueSelected);
                return true;
            } else if ("File|New|Resource|Mail Session".equals(menuSelected)) {
                navigator.addNewMailSession(valueSelected);
                return true;
            } else if ("File|New|Resource|Cloud Bucket|AWS S3".equals(menuSelected)) {
            	navigator.addNewAWSS3(valueSelected);
            	return true;
            } else if ("File|Import...".equals(menuSelected)) {
                navigator.doImport();
                return true;
            } else if ("File|Export...".equals(menuSelected)) {
                navigator.doExport(valueSelected);
                return true;
            } else if ("File|Open".equals(menuSelected)) {
                navigator.doOpen(valueSelected);
                return true;
            } else if ("File|Where Used".equals(menuSelected)) {
                navigator.doWhereUsed(valueSelected);
                return true;
            } else if ("Edit|Remove".equals(menuSelected)) {
                navigator.doRemove(valueSelected);
                return true;
            } else if ("Edit|Cut".equals(menuSelected)) {
                navigator.doCut(valueSelected);
                return true;
            } else if ("Edit|Copy".equals(menuSelected)) {
                navigator.doCopy(valueSelected);
                return true;
            } else if ("Edit|Paste".equals(menuSelected)) {
                navigator.doPaste(valueSelected);
            } else if ("Edit|Change Dependency Version".equals(menuSelected)) {
                navigator.doChangeDependencyVersion(valueSelected);
            } else if ("Tag".equals(menuSelected)) {
                navigator.doTag(valueSelected);
            }
        }
        return handled;
    }
    
    protected boolean isReadOnly(AbstractNamedObject selected) {
        ProjectVersion projectVersion = navigator.findProjectVersion(selected);
        if (projectVersion != null) {
            return projectVersion.locked();
        } else {
            return false;
        }
    }
    
    protected String[] getDisabledPaths(AbstractNamedObject selected) {
        if (isReadOnly(selected)) {
            return new String[] { "File|New|Project Dependency", "File|New|Flow|Design", "File|New|Flow|Test", 
                    "File|New|Model|Hierarchical", "File|New|Model|Relational",
                    "File|New|Resource|Database", "File|New|Resource|Directory|FTP", "File|New|Resource|Directory|File System",
                    "File|New|Resource|Directory|JMS", "File|New|Resource|Directory|SFTP", "File|New|Resource|Directory|SMB",
                    "File|New|Resource|HTTP", "File|New|Resource|Mail Session", "File|New|Resource|Subscribe|JMS", 
                    "File|New|Resource|Cloud Bucket|AWS S3","Edit|Cut", "Edit|Paste", "Edit|Rename", "Tag" };
        } else {
            return null;
        }
    }    

    @Override
    protected String[] getEnabledPaths(Object selected) {        
        return new String[] { "File|New|Project", "View|Hidden", "File|Import..." };
    }

}
