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

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.jumpmind.metl.core.runtime.component.helpers.MessageTestHelper;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.properties.TypedProperties;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
public class UnzipTest extends AbstractComponentRuntimeTestSupport {

	@Override
	public void testStartDefaults() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testStartWithValues() {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void testHandleStartupMessage() {
		MessageTestHelper.addControlMessage(this, "test", false);
		runHandle();
		assertHandle(0);
	}

	@Override
	public void testHandleUnitOfWorkLastMessage() {
		setupHandle();
		
		MessageTestHelper.addControlMessage(this, "test", true);
		MessageTestHelper.addOutputMonitor(this, MessageTestHelper.nullMessage());
		runHandle();
		assertHandle(0);
	}

	@Override
	public void testHandleNormal() {
		// Setup
		setupHandle(true);
		
		MessageTestHelper.addInputMessage(this, true, "step1", "test.zip");
		
		// Expected
		MessageTestHelper.addOutputMonitor(this, "");
		
		runHandle();
		assertHandle(0);
	}

	@Override
	protected String getComponentId() {
		return UnZip.TYPE;
	}

	public void setupHandle(boolean fileExists) {
		super.setupHandle();
		
		IResourceRuntime mockResourceRuntime = mock(IResourceRuntime.class);
		IDirectory mockStreamable = mock(IDirectory.class);
		TypedProperties mockTypedProperties = mock(TypedProperties.class);
		File mockFile = mock(File.class);
		ZipFile mockZipFile = mock(ZipFile.class);
		ZipEntry mockZipEntry = mock(ZipEntry.class);
		ZipEntryIterator iterator = new ZipEntryIterator();
		List<ZipEntry> entries = new ArrayList<ZipEntry>();
		entries.add(mockZipEntry);
		entries.add(mockZipEntry);
		entries.add(mockZipEntry);
		
		iterator.setEntries(entries);
		
		when(((UnZip) spy).getResourceRuntime()).thenReturn(mockResourceRuntime);
		when(mockResourceRuntime.getResourceRuntimeSettings()).thenReturn(mockTypedProperties);
		when(mockTypedProperties.get(anyString())).thenReturn("localFilePath");
		
		when(((UnZip) spy).getResourceReference()).thenReturn(mockStreamable);
		when(((UnZip) spy).getNewFile(anyString())).thenReturn(mockFile);
		try {
			when(((UnZip) spy).getNewZipFile(Mockito.any(File.class))).thenReturn(mockZipFile);
		}
		catch(IOException ioe) {
		}
		when(mockFile.getAbsolutePath()).thenReturn("fileAbsolutePath");
		when(mockFile.exists()).thenReturn(fileExists);
		
	}
	
	class ZipEntryIterator implements Enumeration<ZipEntry>, Iterator<ZipEntry> {
		private int counter = 0;
		private List<ZipEntry> entries;
		
		@Override
		public boolean hasNext() {
			return counter < entries.size();
		}

		@Override
		public ZipEntry next() {
			return entries.get(counter);
		}

		@Override
		public boolean hasMoreElements() {
			return counter < entries.size();
		}

		@Override
		public ZipEntry nextElement() {
			return entries.get(counter);
		}

		List<ZipEntry> getEntries() {
			return entries;
		}

		void setEntries(List<ZipEntry> entries) {
			this.entries = entries;
		}
	
	}
}
