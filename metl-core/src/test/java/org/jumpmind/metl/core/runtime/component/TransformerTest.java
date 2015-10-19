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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.ControlMessage;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentAttributeSettingsBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ModelHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelAttributeScriptHelper.class)
public class TransformerTest extends AbstractComponentRuntimeTestSupport<ArrayList<EntityData>> {
	
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
		setInputMessage(new ControlMessage());
		// Expected
		Message expectedMessage1 = new MessageBuilder().withPayload(
				new PayloadBuilder().buildED()).build();
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0, false, expectedMessage1));
	}

	@Test 
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		// Expected
		Message expectedMessage1 = new MessageBuilder().withPayload(
				new PayloadBuilder().buildED()).build();
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitor(0, 0, false, expectedMessage1));
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
				MODEL_ATTR_ID_1, MODEL_ENTITY_NAME_1);
		
		// Messages
		Message message1 = new MessageBuilder("step1")
				.withPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, TRANSFORM_SOURCE)
				.build()).buildED()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		// Expected
		Message expectedMessage1 = new MessageBuilder().withPayload(
				new PayloadBuilder()
				.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, TRANSFORM_RESULT)
						.build()).buildED()).build();
		
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(0, 0, false, expectedMessage1));
		
		// Execute and Assert
		runHandle();
		assertHandle(1, expectedMonitors);
	}

    @Override
    protected String getComponentId() {
        return Transformer.TYPE;
    }

	@Override
	public void setupHandle() {
		super.setupHandle();
		
		ArrayList<EntityData> payload = new ArrayList<EntityData>(); 
		getInputMessage().setPayload(payload);
	}

	
}
