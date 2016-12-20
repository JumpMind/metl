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
package org.jumpmind.metl.core.runtime.resource;

import org.jumpmind.properties.TypedProperties;

public class LocalFile extends AbstractResourceRuntime {

	public static final String TYPE = "Local File System";

	public final static String LOCALFILE_PATH = "localfile.path";

	public static final String LOCALFILE_MUST_EXIST = "localfile.must.exist";

	IDirectory streamableResource;

	@Override
	protected void start(TypedProperties properties) {
		streamableResource = new LocalFileDirectory(resource,
				properties.getProperty(LOCALFILE_PATH), 
				properties.is(LOCALFILE_MUST_EXIST));
	}

	@Override
	public void stop() {
		streamableResource.close();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T reference() {
		return (T) streamableResource;
	}
	
}