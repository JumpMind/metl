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

import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.runtime.AbstractRuntimeObject;
import org.jumpmind.properties.TypedProperties;

public abstract class AbstractResourceRuntime extends AbstractRuntimeObject implements IResourceRuntime {

    protected Resource resource;
    protected TypedProperties resourceRuntimeSettings;

    @Override
    public void start(Resource resource,
            TypedProperties resourceRuntimeSettings) {
        this.resource = resource;
        this.resourceRuntimeSettings = resourceRuntimeSettings;
        start(resourceRuntimeSettings);
    }
    
    protected void start(TypedProperties properties) {}
    
    @Override
    public void stop() {
    }

    @Override
    public Resource getResource() {
        return resource;
    }

    @Override
    public TypedProperties getResourceRuntimeSettings() {
        return resourceRuntimeSettings;
    }    
    
    @Override
    public boolean isTestSupported() {
        return false;
    }
    
    @Override
    public boolean test() {
        throw new RuntimeException("This resource does not support testing.");
    }

}
