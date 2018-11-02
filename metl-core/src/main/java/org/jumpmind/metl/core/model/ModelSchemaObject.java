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

import java.util.ArrayList;
import java.util.List;

public class ModelSchemaObject extends AbstractNamedObject implements IAuditable {

    private static final long serialVersionUID = 1L;

    String modelId;

    String name;
    
    String description;

    String type;
    
    String refModelId;
    
    String parentId;
    
    String pattern;
    
    int minLength;
    
    int maxLength;
    
    int minimum;
    
    int maximum;
    
    boolean required;
    
    List<ModelSchemaObject> childObjects;
    
    public ModelSchemaObject() {
        childObjects = new ArrayList<ModelSchemaObject>();
    }

    public ModelSchemaObject(String id, String name) {
        this();
        setId(id);
        this.name = name;
    }

    public String getModelId() {
        return modelId;
    }

    public void setModelId(String modelVersionId) {
        this.modelId = modelVersionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public int getMinLength() {
        return minLength;
    }

    public void setMinLength(int minLength) {
        this.minLength = minLength;
    }

    public int getMaxLength() {
        return maxLength;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    public int getMinimum() {
        return minimum;
    }

    public void setMinimum(int minimum) {
        this.minimum = minimum;
    }

    public int getMaximum() {
        return maximum;
    }

    public void setMaximum(int maximum) {
        this.maximum = maximum;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public List<ModelSchemaObject> getChildObjects() {
        return childObjects;
    }

    public void setChildObjects(List<ModelSchemaObject> childObjects) {
        this.childObjects = childObjects;
    }

    public String getRefModelId() {
        return refModelId;
    }

    public void setRefModelId(String refModelId) {
        this.refModelId = refModelId;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
    }

}
