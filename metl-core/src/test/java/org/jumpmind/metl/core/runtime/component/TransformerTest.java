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
package org.jumpmind.metl.core.runtime.component;

import java.util.HashMap;
import java.util.Map;

import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentAttributeSettingsBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.ModelHelper;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelAttributeScriptHelper.class)
public class TransformerTest extends AbstractComponentRuntimeTestSupport {
	
	public static String TRANSFORM_SOURCE = "transform me";
	public static String TRANSFORM_EXP = "transform logic";
	public static String TRANSFORM_RESULT = "transformed";
	
	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new ComponentBuilder().withAttributeSettings(
				new ComponentAttributeSettingsBuilder().build()).build());
		
		((Transformer) spy).start();
	}

	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new ComponentBuilder().withAttributeSettings(
			new ComponentAttributeSettingsBuilder()
			.withSetting(MODEL_ATTR_ID_1, "Component1", Transformer.TRANSFORM_EXPRESSION, "value == 'A'")
			.build()).build());
		
		((Transformer) spy).start();
		Assert.assertTrue(((Transformer) spy).transformsByAttributeId.containsKey(MODEL_ATTR_ID_1));
		Assert.assertEquals("value == 'A'", ((Transformer) spy).transformsByAttributeId.get(MODEL_ATTR_ID_1));
		
	}
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		MessageTestHelper.addControlMessage(this, "test", false);
		runHandle();
		assertHandle(0);
	}

	@Test 
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		runHandle();
		assertHandle(0);
	}

	@Test
	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle();
		PowerMockito.mockStatic(ModelAttributeScriptHelper.class);
		try {
			PowerMockito.doReturn(TRANSFORM_RESULT).when(
				ModelAttributeScriptHelper.class, 
				"eval",
				Mockito.any(Message.class), 
				Mockito.any(ComponentContext.class), 
				Mockito.any(ModelAttribute.class), 
				Mockito.anyObject(), 
				Mockito.any(ModelEntity.class), 
				Mockito.any(EntityData.class), 
				Mockito.anyString());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		Map<String, String> transformMap = new HashMap<String, String>();
		transformMap.put(MODEL_ATTR_ID_1, TRANSFORM_EXP);
		((Transformer) spy).transformsByAttributeId = transformMap;
		
		ModelHelper.createMockModel(inputModel, MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1, 
				MODEL_ATTR_ID_1, ENTITY_1_KEY_1);
		
		// Messages
		MessageTestHelper.addInputMessage(this, true, "step1", MODEL_ATTR_ID_1,TRANSFORM_SOURCE);
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, MODEL_ATTR_ID_1, TRANSFORM_RESULT);
		
		// Execute and Assert
		runHandle();
		assertHandle(1);
	}

    @Override
    protected String getComponentId() {
        return Transformer.TYPE;
    }

	
}
