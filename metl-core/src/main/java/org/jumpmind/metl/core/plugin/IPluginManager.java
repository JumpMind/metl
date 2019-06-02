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
package org.jumpmind.metl.core.plugin;

import java.io.File;
import java.util.List;

import org.jumpmind.metl.core.model.Plugin;
import org.jumpmind.metl.core.model.PluginRepository;

public interface IPluginManager {
    
    public void init();
    
    public void refresh();
    
    public String toPluginId(String artifactGroup, String artifactName, String artifactVersion);
    
    public List<String> getAvailableVersions(String artifactGroup, String artifactName, List<PluginRepository> remoteRepositories);
    
    public String getLatestLocalVersion(String artifactGroup, String artifactName);
    
    public ClassLoader getClassLoader(String artifactGroup, String artifactName, String artifactVersion, List<PluginRepository> remoteRepositories);

    List<Plugin> getOutOfTheBox();

    boolean isNewer(Plugin first, Plugin second);

    void delete(String artifactGroup, String artifactName, String artifactVersion);
    
    public void install(String artifactGroup, String artifactName, String artifactVersion, File file);

}
