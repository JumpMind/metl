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
import java.util.HashSet;
import java.util.Set;

import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentAttributeSettingsBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.ModelBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.ModelHelper;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;



@RunWith(PowerMockRunner.class)
public class MappingTest extends AbstractComponentRuntimeTestSupport {

	public static String MAPPING_TARGET_1 = "mapping1";
	
	@Test
	@Override
	public void testStartDefaults() {
		inputModel = null;
		outputModel = null;
		setupStart(new SettingsBuilder().build());
		try {
			((Mapping) spy).start();
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof MisconfiguredException);
		}
	}

	@Test
	@Override
	public void testStartWithValues() {
		inputModel = new ModelBuilder().build();
		outputModel = new ModelBuilder().build();
		
		setupStart(new ComponentBuilder().withAttributeSettings(new ComponentAttributeSettingsBuilder()
				.withSetting(MODEL_ATTR_ID_1, "Component1", Mapping.ATTRIBUTE_MAPS_TO, "mappedValue")
				.build()).build());
		
		((Mapping) spy).start();
		Assert.assertTrue(((Mapping) spy).attrToAttrMap.containsKey(MODEL_ATTR_ID_1));
		Assert.assertTrue(((Mapping) spy).attrToAttrMap.get(MODEL_ATTR_ID_1).contains("mappedValue"));
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
	public void testHandleNormal() throws Exception {
		// Setup
		setupHandle();
		Set<String> mappings = new HashSet<String>();
		mappings.add(MAPPING_TARGET_1);
		
		((Mapping) spy).attrToAttrMap = new HashMap<String, Set<String>>();
		((Mapping) spy).attrToAttrMap.put(MODEL_ATTR_ID_1, mappings);
		
		// Messages
		MessageTestHelper.addInputMessage(this, true, "step1", MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, MAPPING_TARGET_1, MODEL_ATTR_NAME_1);
		
		// Execute and Assert
		runHandle();
		assertHandle(1);
	}
	
	@Test 
	public void testHandleUnMappedToNull() {
		// Setup
		setupHandle();
		Set<String> mappings = new HashSet<String>();
		mappings.add(MAPPING_TARGET_1);
		
		((Mapping) spy).attrToAttrMap = new HashMap<String, Set<String>>();
		((Mapping) spy).attrToAttrMap.put("X", mappings);
		((Mapping) spy).setUnmappedAttributesToNull = true;
		
		// Messages
		MessageTestHelper.addInputMessage(this, true, "step1", MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, MODEL_ATTR_ID_1, null);
		
		ModelHelper.createMockModel(outputModel, MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1, ENTITY_1_KEY_1, ENTITY_1_VALUE_1);
		
		// Execute and Assert
		runHandle();
		assertHandle(1); 
	}
	
	@Test 
	public void testHandleNoMappingsFound() {
		// Setup
		setupHandle();
		Set<String> mappings = new HashSet<String>();
		mappings.add(MAPPING_TARGET_1);
		((Mapping) spy).attrToAttrMap = new HashMap<String, Set<String>>();
		((Mapping) spy).attrToAttrMap.put("X", mappings);
		
		// Messages
		MessageTestHelper.addInputMessage(this, true, "step1", MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1);
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		
		// Execute and Assert
		runHandle();
		assertHandle(0);
	}
	
    @Override
    protected String getComponentId() {
        return Mapping.TYPE;
    }

}
