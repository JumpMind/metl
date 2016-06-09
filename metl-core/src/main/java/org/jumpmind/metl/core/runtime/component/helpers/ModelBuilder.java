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

import java.util.ArrayList;

import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelEntity;

public class ModelBuilder {
	Model model;
	
	public ModelBuilder() {
		model = new Model();
		model.setModelEntities(new ArrayList<ModelEntity>());
	}
	
	public ModelBuilder(Model m) {
		model = m;
		if (model.getModelEntities() == null) {
			model.setModelEntities(new ArrayList<ModelEntity>());
		}
	}
	public ModelBuilder withFolder(Folder folder) {
		model.setFolder(folder);
		return this;
	}
	
	public ModelBuilder withId(String id) {
		model.setId(id);
		return this;
	}

	public ModelBuilder withName(String name) {
		model.setName(name);
		return this;
	}
	
	public ModelBuilder withProjectVersionId(String id) {
		model.setProjectVersionId(id);
		return this;
	}
	
	public ModelBuilder withShared(boolean shared) {
		model.setShared(shared);
		return this;
	}
	
	public ModelBuilder withDeleted(boolean deleted) {
		model.setDeleted(deleted);
		return this;
	}
	
	public ModelBuilder withEntity(ModelEntity entity) {
		this.model.getModelEntities().add(entity);
		return this;
	}
	
	public Model build() {
		return this.model;
	}
	
	
	
}
