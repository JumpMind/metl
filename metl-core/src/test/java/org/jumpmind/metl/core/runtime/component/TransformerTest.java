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
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.StartupMessage;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ModelHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelAttributeScriptHelper.class)
public class TransformerTest extends AbstractComponentRuntimeTest<ArrayList<EntityData>> {
	
	public static String TRANSFORM_SOURCE = "transform me";
	public static String TRANSFORM_EXP = "transform logic";
	public static String TRANSFORM_RESULT = "transformed";
	
	@Test
	@Override
	public void testHandleStartupMessage() {
		setInputMessage(new StartupMessage());
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(1, 0, 0, 0));
	}

	@Test 
	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		setUnitOfWorkLastMessage(true);
		
		getInputMessage().setPayload(new ArrayList<EntityData>());
		
		runHandle();
		assertHandle(0, getExpectedMessageMonitorSingle(1, 0, 0, 0));
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
				.setPayload(new PayloadBuilder()
					.addRow(new EntityDataBuilder()
						.withKV(MODEL_ATTR_ID_1, TRANSFORM_SOURCE)
				.build()).buildED()).build();
		
		messages.clear();
		messages.add(new HandleParams(message1, true));
		
		// Expected
		ArrayList<EntityData> expectedPayload = new PayloadBuilder()
						.addRow(new EntityDataBuilder()
							.withKV(MODEL_ATTR_ID_1, TRANSFORM_RESULT)
						.build()).buildED();
		
		
		List<HandleMessageMonitor> expectedMonitors = new ArrayList<HandleMessageMonitor>();
		expectedMonitors.add(getExpectedMessageMonitor(1, 0, 0, 1, expectedPayload));
		
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
