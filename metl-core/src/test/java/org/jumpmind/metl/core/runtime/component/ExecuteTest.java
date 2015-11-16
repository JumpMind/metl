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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;

import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.component.helpers.SettingsBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ModelAttributeScriptHelper.class)
public class ExecuteTest extends AbstractComponentRuntimeTestSupport {

	public static final String COMMAND_OUTPUT = "command output";
	
	@Test
	@Override
	public void testStartDefaults() {
		setupStart(new SettingsBuilder().build());
		try {
			((Execute) spy).start();			
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IllegalStateException);
		}
	}

	@Test
	@Override
	public void testStartWithValues() {
		setupStart(new SettingsBuilder()
				.withSetting(Execute.COMMAND, "top")
				.withSetting(Execute.CONTINUE_ON_ERROR, "true")
				.withSetting(Execute.SUCCESS_CODE, "1").build());
		
		((Execute) spy).start();
		
		String[] actual = ((Execute) spy).commands;
		String[] expected = new String[] { "top" };
		
		Assert.assertArrayEquals(expected, actual);
		Assert.assertTrue(((Execute) spy).continueOnError);
		Assert.assertEquals(1, ((Execute) spy).successCode);
	}
	
	@Test
	public void testStartDetaultsWithCommand() {
		setupStart(new SettingsBuilder()
				.withSetting(Execute.COMMAND, "top").build());
		((Execute) spy).start();
		
		Assert.assertFalse(((Execute) spy).continueOnError);
		Assert.assertEquals(0, ((Execute) spy).successCode);
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
		setupHandle(0);
		
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		runHandle();
		assertHandle(0);
	}

	@Test
	@Override
	public void testHandleNormal() {
		setupHandle(0);
		
		MessageTestHelper.addInputMessage(this, true, "step1", COMMAND_OUTPUT);
		MessageTestHelper.addOutputMonitor(this, COMMAND_OUTPUT);
				
		runHandle();
		assertHandle(0);
	}
	
	@Test
	public void testHandleError() {
		setupHandle(1);
		
		MessageTestHelper.addInputMessage(this, true, "step1", COMMAND_OUTPUT);
		MessageTestHelper.addOutputMonitor(this, COMMAND_OUTPUT);
				
		try {
			runHandle();
			Assert.fail("Should of thrown IOException");
		}
		catch (Exception e) {
			Assert.assertTrue(e instanceof IoException);
		}
	}
	
	@Test
	public void testHandleErrorContinue() {
		setupHandle(1);
		((Execute) spy).continueOnError = true;
		
		MessageTestHelper.addInputMessage(this, true, "step1", COMMAND_OUTPUT);
		MessageTestHelper.addOutputMonitor(this, COMMAND_OUTPUT);
				
		runHandle();
		assertHandle(0);
		
	}

	@Override
	protected String getComponentId() {
		return Execute.TYPE;
	}
	
	public void setupHandle(int outputError) {
		super.setupHandle();
		
		org.apache.tools.ant.taskdefs.Execute mockExecute = mock(org.apache.tools.ant.taskdefs.Execute.class);
		ByteArrayOutputStream mockOutputStream = mock(ByteArrayOutputStream.class);
		
		try {
			when(((Execute) spy).getAntTask(Mockito.any(PumpStreamHandler.class))).thenReturn(mockExecute);
			when(((Execute) spy).getByteArrayOutputStream()).thenReturn(mockOutputStream);
			when(mockExecute.execute()).thenReturn(outputError);
			when(mockOutputStream.toByteArray()).thenReturn(COMMAND_OUTPUT.getBytes());
			((Execute) spy).setRunWhen(Execute.PER_MESSAGE);
		}
		catch (Exception e) {
			Assert.fail("Unable to execute " + e.getMessage());
		}
	}

}
