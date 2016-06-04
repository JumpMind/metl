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


public class FlowStep extends AbstractObject {

    private static final long serialVersionUID = 1L;    
    
    protected Component component;
    
    String flowId;
    
    int x;
    
    int y;
    
    int approximateOrder;
    
    public FlowStep() {
    }

    public FlowStep(Component component) {
        this.component = component;
    }

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }
    
    public void setName(String name) {
        this.component.setName(name);
    }
    
    public String getName() {
        return this.component.getName();
    }
    
    public String getComponentId() {
        return component != null ? component.getId() : null;
    }

    public void setComponentId(String componentId) {
        if (componentId != null) {
            this.component = new Component(componentId);
        } else {
            this.component = null;
        }
    }

    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }
    
    public void setX(int x) {
        this.x = x;
    }
    
    public int getX() {
        return x;
    }
    
    public void setY(int y) {
        this.y = y;
    }
    
    public int getY() {
        return y;
    }
    
    public void setApproximateOrder(int order) {
        this.approximateOrder = order;
    }
    
    public int getApproximateOrder() {
        return approximateOrder;
    }

    @Override
    public boolean isSettingNameAllowed() {
        return true;
    }
    
}
