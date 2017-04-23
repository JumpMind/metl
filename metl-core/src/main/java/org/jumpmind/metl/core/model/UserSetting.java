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
package org.jumpmind.metl.core.model;

public class UserSetting extends Setting {

    private static final long serialVersionUID = 1L;
    
    public static final String SETTING_SHOW_RUN_DIAGRAM = "show.run.diagram";
    public static final String SETTING_MAX_LOG_MESSAGE_TO_SHOW = "max.log.messages.to.show";
    public static final String SETTING_DESIGN_NAVIGATOR_SELECTION_ID = "design.navigator.selection.id";
    public static final String SETTING_DESIGN_NAVIGATOR_EXPANDED_IDS = "design.navigator.expanded.ids";
    public static final String SETTING_DESIGN_NAVIGATOR_SELECTED_PROJECT_ID = "design.navigator.selected.project.id";
    public static final String SETTING_FLOW_PANEL_VIEW_VERTICAL = "flow.panel.view.vertical";

    String userId;
    
    public UserSetting() {
    }
    
    public UserSetting(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
    
}
