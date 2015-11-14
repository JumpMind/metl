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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class TextReplaceTest extends AbstractComponentRuntimeTestSupport {
	

	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder()
				.withSetting(TextReplace.SETTING_SEARCH_FOR, "")
				.withSetting(TextReplace.SETTING_REPLACE_WITH, "").build());
		
		try {
			((TextReplace) spy).start();
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}
		
		assertEquals("", ((TextReplace) spy).searchFor);
		assertEquals("", ((TextReplace) spy).replaceWith);
	}
	
	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
				.withSetting(TextReplace.SETTING_SEARCH_FOR, "search")
				.withSetting(TextReplace.SETTING_REPLACE_WITH, "replace").build());
		 
		((TextReplace) spy).start();
		
		assertEquals("search", ((TextReplace) spy).searchFor);
		assertEquals("replace", ((TextReplace) spy).replaceWith);
	}
	
	@Test
	public void testStartWithMissingProperties() {
		setupStart(new SettingsBuilder()
			.withSetting(TextReplace.SETTING_SEARCH_FOR, null)
			.withSetting(TextReplace.SETTING_REPLACE_WITH, "replace").build());
				
		try {
			((TextReplace) spy).start();
		}
		catch (Exception e) {
			assertTrue(e instanceof IllegalStateException);
		}
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
		((TextReplace) spy).searchFor = "replaceMe";
		((TextReplace) spy).replaceWith = "replaced";
		
		MessageTestHelper.addInputMessage(this, false, "step1", "Someone please replaceMe");
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, "Someone please replaced");
		
		runHandle();
		assertHandle(1);
	}
	
	@Test
	public void testHandleNoMatches() {
		// Setup
		setupHandle();
		((TextReplace) spy).searchFor = "replaceMe";
		((TextReplace) spy).replaceWith = "replaced";
		
		MessageTestHelper.addInputMessage(this, false, "step1", "Someone please replace me");
		
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, "Someone please replace me");
		
		runHandle();
		assertHandle(1);
	}

	@Override
	protected String getComponentId() {
		return TextReplace.TYPE;
	}

}
