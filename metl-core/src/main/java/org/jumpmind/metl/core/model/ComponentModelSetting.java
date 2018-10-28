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

public class ComponentModelSetting extends Setting implements IAuditable {

    public enum Type {ENTITY,ATTRIBUTE,SCHEMA_OBJECT}

    private static final long serialVersionUID = 1L;
    
    String type;
    String componentId;
    String modelObjectId;

    public ComponentModelSetting() {
    }
    
    public ComponentModelSetting(String type, String modelObjectId, String componentId, String name, String value) {
        super(name, value);
        this.type = type;
        this.componentId = componentId;
        this.modelObjectId = modelObjectId;
    }

    public ComponentModelSetting(String type, String modelObjectId, String name, String value) {
        super(name, value);
        this.type = type;
        this.modelObjectId = modelObjectId;
    }
    
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentId() {
        return componentId;
    }

    public String getModelObjectId() {
        return modelObjectId;
    }

    public void setModelObjectId(String modelObjectId) {
        this.modelObjectId = modelObjectId;
    }

}
