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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.mockito.Mockito;

public class ModelHelper {
	
	public static final String MODEL_ID = "MODEL_ID";
	public static final String MODEL_NAME = "MODEL_NAME";
	
	public static final String ENTITY_ID = "ENTITY_ID";
	public static final String ENTITY_NAME = "ENTITY_NAME";
	
	public static final String ATTR_ID = "ATTR_ID";
	public static final String ATTR_NAME = "ATTR_NAME";
	
	public static void createMockModel(Model target, String attrId, String attrName, String entityId, String entityName) {
		 	ModelAttribute attr = mock(ModelAttribute.class);
			ModelEntity entity = mock(ModelEntity.class);
			List<ModelEntity> entities = new ArrayList<ModelEntity>();
			List<ModelAttribute> attributes = new ArrayList<ModelAttribute>();
			
			when(attr.getId()).thenReturn(attrId);
			when(attr.getName()).thenReturn(attrName);
			when(attr.getEntityId()).thenReturn(entityId);
			
			when(entity.getModelAttributes()).thenReturn(attributes);
			when(entity.getId()).thenReturn(entityId);
			when(entity.getName()).thenReturn(entityName);
			
			attributes.add(attr);
			entities.add(entity);
		
			when(target.getModelEntities()).thenReturn(entities);
			when(target.getAttributeById(Mockito.eq(attrId))).thenReturn(attr);
	}
	
	public static Model createSimpleModel(int numberEntities, int attrPerEntity) {
		ModelBuilder builder = new ModelBuilder()
				.withDeleted(false)
				.withName(MODEL_NAME)
				.withId(MODEL_ID);
		
		for (int e = 0; e < numberEntities; e++) {
			ModelEntityBuilder eBuilder = new ModelEntityBuilder()
				.withModelId(MODEL_ID)
				.withName(ENTITY_NAME + e)
				.withId(ENTITY_ID + e);
			
			for (int a = 0; a < attrPerEntity; a++) {
				eBuilder = eBuilder.withAttribute(new ModelAttributeBuilder()
					.withEntityId(ENTITY_ID + e)
					.withName(ATTR_NAME)
					.withId(ATTR_ID + a + "-" + e).build());
			}
			builder = builder.withEntity(eBuilder.build());
		}
		return builder.build();
		
		
	}
}
