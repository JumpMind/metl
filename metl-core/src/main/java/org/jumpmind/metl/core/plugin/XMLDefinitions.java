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
package org.jumpmind.metl.core.plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "definitions", propOrder = {
    "component", "resource"
})
public class XMLDefinitions implements Serializable {

    private static final long serialVersionUID = 1L;

    @XmlElement(required = true)
    protected List<XMLComponentDefinition> component;
    
    @XmlElement(required = true)
    protected List<XMLResourceDefinition> resource;    
    
    public void setComponent(List<XMLComponentDefinition> components) {
        this.component = components;
    }
    
    public List<XMLComponentDefinition> getComponent() {
        if (component == null) {
            component = new ArrayList<>();
        }
        return component;
    }
    
    public void setResource(List<XMLResourceDefinition> resource) {
        this.resource = resource;
    }
    
    public List<XMLResourceDefinition> getResource() {
        if (resource == null) {
            resource = new ArrayList<>();
        }
        return resource;
    }

}
