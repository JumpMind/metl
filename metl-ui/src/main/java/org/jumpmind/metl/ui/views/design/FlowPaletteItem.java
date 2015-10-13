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
package org.jumpmind.metl.ui.views.design;

import com.vaadin.ui.Button;

public class FlowPaletteItem extends Button {
    
    private static final long serialVersionUID = 1L;
    
    String componentType;
    
    String componentId;
    
    boolean isShared;

    public FlowPaletteItem(String label) {
        super(label);
        addStyleName("hidefocus");
    }
    
    public void setComponentType(String componentType) {
        this.componentType = componentType;
    }
    
    public String getComponentType() {
        return componentType;
    }
    
    public String getComponentId() {
        return componentId;
    }
    
    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }
    
    public boolean isShared() {
        return isShared;
    }
    
    public void setShared(boolean isShared) {
        this.isShared = isShared;
    }
}
