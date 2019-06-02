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

import java.util.List;

public class Tag extends AbstractNamedObject implements IAuditable {

    private static final long serialVersionUID = 1L;
    
    String name;
    
    int color;

    List<EntityTag> taggedItems;

    public Tag() {
        
    }
    
    public Tag(String id, String name, int color) {
        this.setId(id);
        this.name = name;
        this.color = color;
    }
    
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    public List<EntityTag> getTaggedItems() {
        return taggedItems;
    }

    public void setTaggedItems(List<EntityTag> taggedItems) {
        this.taggedItems = taggedItems;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

}