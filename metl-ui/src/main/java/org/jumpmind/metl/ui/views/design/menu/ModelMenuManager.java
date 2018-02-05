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

import org.apache.commons.lang.ArrayUtils;
import org.jumpmind.metl.ui.common.CutCopyPasteManager;
import org.jumpmind.metl.ui.views.design.DesignNavigator;

public class ModelMenuManager extends AbstractDesignSelectedValueMenuManager {

    public ModelMenuManager(DesignNavigator navigator) {
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
            return (String[])ArrayUtils.addAll(super.getDisabledPaths(selected), new String[] { "Edit|Remove"
            });            
        } else {
            return super.getDisabledPaths(selected);
        }
    }
    
    @Override
    protected String[] getEnabledPaths(Object selected) {
        
        String[] enabledPaths = (String[]) ArrayUtils.addAll(super.getEnabledPaths(selected), new String[] {
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
                "File|Open",
                "File|Import...",        
                "File|Export...",                
                "Edit|Rename",
                "Edit|Cut",
                "Edit|Copy",
                "Edit|Remove"
        });        
        if (navigator.getContext().getClipboard()
                .containsKey(CutCopyPasteManager.CLIPBOARD_OBJECT_TYPE)) {
            enabledPaths = (String[]) ArrayUtils.add(enabledPaths, "Edit|Paste");
        }
        return enabledPaths; 
    }
}
