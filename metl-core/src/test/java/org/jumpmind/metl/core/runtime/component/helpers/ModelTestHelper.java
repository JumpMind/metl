package org.jumpmind.metl.core.runtime.component.helpers;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.mockito.Mockito;

public class ModelTestHelper {

	
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
}
