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

import org.jumpmind.metl.core.model.SettingDefinition;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent.ResourceCategory;
import org.jumpmind.metl.core.runtime.component.definition.XMLSetting.Type;
import org.jumpmind.properties.TypedProperties;

@ResourceDefinition(typeName=LocalFile.TYPE, resourceCategory=ResourceCategory.STREAMABLE)
public class LocalFile extends AbstractResourceRuntime {

	public static final String TYPE = "Local File System";

	@SettingDefinition(order = 0, required = false, type = Type.TEXT, label = "Path")
	public final static String LOCALFILE_PATH = "localfile.path";

	@SettingDefinition(type = Type.BOOLEAN, order = 20, required = true, provided = true, defaultValue = "false", label = "Must Exist")
	public static final String LOCALFILE_MUST_EXIST = "localfile.must.exist";

	IStreamable streamableResource;

	@Override
	protected void start(TypedProperties properties) {
		streamableResource = new LocalFileStreamable(resource,
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
		// TODO think about renaming reference to resource or something more meaningful
		return (T) streamableResource;
	}
	
}