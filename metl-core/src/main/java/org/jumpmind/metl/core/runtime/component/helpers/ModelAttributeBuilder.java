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
package org.jumpmind.metl.core.runtime.component.helpers;

import org.jumpmind.metl.core.model.ModelAttrib;

public class ModelAttributeBuilder {
	ModelAttrib attribute = new ModelAttrib();
	
	public ModelAttributeBuilder() {
		
	}
	
	public ModelAttributeBuilder withEntityId(String entityId) {
		attribute.setEntityId(entityId);
		return this;
	}
	
	public ModelAttributeBuilder withId(String id) {
		attribute.setId(id);
		return this;
	}
	public ModelAttributeBuilder withName(String name) {
		attribute.setName(name);
		return this;
	}
	
	public ModelAttributeBuilder withTypeEntityId(String typeEntityId) {
		attribute.setTypeEntityId(typeEntityId);
		return this;
	}
	
	public ModelAttributeBuilder withType(String type) {
		attribute.setType(type);
		return this;
	}
	
	public ModelAttrib build() {
		return attribute;
	}
}
