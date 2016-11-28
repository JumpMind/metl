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

import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;

public class ComponentRuntimeFactory implements IComponentRuntimeFactory {

    IDefinitionFactory componentDefinitionFactory;

    public ComponentRuntimeFactory(IDefinitionFactory componentDefinitionFactory) {
        this.componentDefinitionFactory = componentDefinitionFactory;
    }

    @Override
    synchronized public IComponentRuntime create(String projectVersionId, String id, ComponentContext context, int threadNumber) {
        try {
            XMLComponentDefinition definition = componentDefinitionFactory.getComponentDefinition(projectVersionId, id);
            if (definition != null) {
                IComponentRuntime component = (IComponentRuntime) Class.forName(definition.getClassName().trim(), true, definition.getClassLoader()).newInstance();
                component.create(definition, context, threadNumber);
                return component;
            } else {
                throw new IllegalStateException("Could not find a class associated with the component id of " + id);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
