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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.SimpleBindings;

import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.core.runtime.Message;
import org.jumpmind.metl.core.runtime.flow.ISendMessageCallback;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class AbstractComponentRuntimeTest {
	
	@Test
	public void testBindStringData() {
		AbstractComponentRuntime target = spy(new MockComponent());
		ScriptEngine mockScriptEngine = mock(ScriptEngine.class);
		when(mockScriptEngine.createBindings()).thenReturn(new SimpleBindings());
		Bindings actual = target.bindStringData(mockScriptEngine, "test");
		
		assertTrue(actual.containsKey("text"));
		assertEquals("test", actual.get("text"));
	}
	
	@Test
	public void testBindStringDataNull() {
		AbstractComponentRuntime target = spy(new MockComponent());
		ScriptEngine mockScriptEngine = mock(ScriptEngine.class);
		when(mockScriptEngine.createBindings()).thenReturn(new SimpleBindings());
		doNothing().when(target).log(any(LogLevel.class), anyString());
		
		Bindings actual = target.bindStringData(mockScriptEngine, null);
		assertTrue(!actual.containsKey("text"));
	}
	
	protected class MockComponent extends AbstractComponentRuntime {
		@Override
		public void handle(Message inputMessage, ISendMessageCallback messageTarget, boolean unitOfWorkBoundaryReached) {
			
		}
	
		@Override
		public boolean supportsStartupMessages() {
			return false;
		}
	
		@Override
		protected void start() {
		
		}
	}
}
