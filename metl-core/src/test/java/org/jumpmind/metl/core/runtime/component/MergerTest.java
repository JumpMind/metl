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
import java.util.List;

import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.component.helpers.ComponentAttributeSettingsBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.EntityDataBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.PayloadBuilder;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.jumpmind.metl.core.utils.TestUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class MergerTest extends AbstractComponentRuntimeTestSupport {

    @Test
    @Override
    public void testStartDefaults() {
        setupStart(new SettingsBuilder().build());        
        ((Merger) spy).start();
    }

    @Test
    @Override
    public void testStartWithValues() {
        setupStart(new SettingsBuilder().build());

        ((Merger) spy).getComponent().setInputModel(new Model());
        ((Merger) spy).getComponent().setAttributeSettings(
                new ComponentAttributeSettingsBuilder().withSetting(MODEL_ATTR_ID_1, "1", Merger.MERGE_ATTRIBUTE, "true").build());

        ((Merger) spy).start();

        List<String> expectedList = new ArrayList<String>();
        expectedList.add(MODEL_ATTR_ID_1);

        TestUtils.assertList(expectedList, ((Merger) spy).attributesToMergeOn, false);
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
        // Join setup
        setupHandle();

        List<String> attributesToJoinOn = new ArrayList<String>();
        attributesToJoinOn.add(MODEL_ATTR_ID_1);
        ((Merger) spy).attributesToMergeOn = attributesToJoinOn;

        // Messages
        MessageTestHelper.addInputMessage(this, false, "step1", new EntityDataBuilder().withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1).withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2).build());
        
        MessageTestHelper.addInputMessage(this, true, "step1", new EntityDataBuilder().withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1).withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3).build());
        
        // Expected
        MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
        MessageTestHelper.addOutputMonitor(this, new MessageBuilder("step1").withPayload(
                       	new PayloadBuilder()
                       		.withRow(new EntityDataBuilder().withKV(MODEL_ATTR_ID_1, MODEL_ATTR_NAME_1)
                                 .withKV(MODEL_ATTR_ID_2, MODEL_ATTR_NAME_2)
                                 .withKV(MODEL_ATTR_ID_3, MODEL_ATTR_NAME_3).build())
                        .buildED()).build());

        // Execute and Assert
        runHandle();
        assertHandle(2);
    }

    @Override
    protected String getComponentId() {
        return Merger.TYPE;
    }

}
