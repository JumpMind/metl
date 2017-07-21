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
package org.jumpmind.metl.ui.common;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.plugin.XMLResourceDefinition;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.vaadin.ui.sqlexplorer.IDb;
import org.jumpmind.vaadin.ui.sqlexplorer.IDbProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbProvider implements IDbProvider, Serializable {

    private static final long serialVersionUID = 1L;

    final protected Logger log = LoggerFactory.getLogger(getClass());

    transient List<IDb> dbs = new ArrayList<>();

    ApplicationContext context;

    public DbProvider(ApplicationContext context) {
        this.context = context;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        dbs = new ArrayList<>();
        refresh();
    }

    public void refresh() {
        refresh(false);
    }
    
    public void refresh(boolean showAllResources) {
        for (IDb db : dbs) {
            if (db instanceof DbResource) {
                ((DbResource) db).close();
            }
        }

        dbs.clear();

        IConfigurationService configurationService = context.getConfigurationService();
        List<Project> projects = configurationService.findProjects();
        for (Project project : projects) {
            ProjectVersion version = project.getMasterVersion();
            if (version != null) {
                XMLResourceDefinition defintion = context.getDefinitionFactory().getResourceDefintion(version.getId(), "Database");
                List<Resource> resources = configurationService.findResourcesByTypes(version.getId(), false, "Database");
                for (Resource resource : resources) {
                    if (resource.getBoolean("show.on.explore.screen", false) || showAllResources) {
                        DbResource db = new DbResource("Design > " + resource.getName(),
                                resource.toTypedProperties(defintion.getSettings().getSetting()));
                        dbs.add(db);
                    }
                }
            }
        }

        IAgentManager agentManager = context.getAgentManager();
        Collection<Agent> agents = agentManager.getAvailableAgents();
        for (Agent agent : agents) {
            if (agent.isShowResourcesInExploreView()) {
                AgentRuntime runtime = agentManager.getAgentRuntime(agent.getId());
                Collection<IResourceRuntime> resources = runtime.getDeployedResources();
                for (IResourceRuntime iResource : resources) {
                    if (iResource.getResource().getType().equals("Database")) {
                        DbResource db = new DbResource(agent.getName() + " > " + iResource.getResource().getName(),
                                iResource.getResourceRuntimeSettings());
                        dbs.add(db);
                    }
                }
            }
        }

        Collections.sort(dbs, new Comparator<IDb>() {
            @Override
            public int compare(IDb o1, IDb o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        dbs.add(0, new MetlDb());
        dbs.add(1, new ExecutionMetlDb());

    }

    @Override
    public List<IDb> getDatabases() {
        return dbs;
    }

    class ExecutionMetlDb implements IDb, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public IDatabasePlatform getPlatform() {
            return context.getExecutionDatabasePlatform();
        }

        @Override
        public String getName() {
            return "Metl DB - Exec";
        }

    }

    class MetlDb implements IDb, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public IDatabasePlatform getPlatform() {
            return context.getConfigDatabasePlatform();
        }

        @Override
        public String getName() {
            return "Metl DB - Config";
        }

    }
}