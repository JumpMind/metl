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
package org.jumpmind.metl.ui.views;

import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.resource.Datasource;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.ui.common.AppConstants;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.jumpmind.symmetric.ui.sqlexplorer.IDb;
import org.jumpmind.symmetric.ui.sqlexplorer.IDbProvider;
import org.jumpmind.symmetric.ui.sqlexplorer.SqlExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.VerticalLayout;

@UiComponent
@Scope("ui")
@TopBarLink(id = "explore", category = Category.EXPLORE, menuOrder = 20, name = "Explore", icon = FontAwesome.DATABASE)
public class ExploreView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Autowired
    IDatabasePlatform platform;

    @Autowired
    ApplicationContext context;
    
    DbProvider dbProvider;
    
    SqlExplorer explorer;

    public ExploreView() {
        setSizeFull();
    }
    
    @PostConstruct
    protected void init () {
        dbProvider = new DbProvider();
        explorer = new SqlExplorer(System.getProperty("java.io.tmpdir"),
                dbProvider, "admin", AppConstants.DEFAULT_LEFT_SPLIT);
        addComponent(explorer);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        dbProvider.refresh();
        explorer.refresh();
    }

    class DbResource implements IDb, Serializable {

        private static final long serialVersionUID = 1L;

        IResourceRuntime resource;

        Agent agent;

        IDatabasePlatform platform;

        public DbResource(Agent agent, IResourceRuntime resource) {
            this.resource = resource;
            this.agent = agent;
        }

        @Override
        public String getName() {
            return agent.getName() + " > " + resource.getResource().getName();
        }

        @Override
        public IDatabasePlatform getPlatform() {
            if (platform == null) {                
                DataSource dataSource = resource.reference();
                platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource,
                        new SqlTemplateSettings(), false, false);
            }
            return platform;
        }
        
        public void close() {
            if (platform != null) {
                BasicDataSource ds = (BasicDataSource)platform.getDataSource();
                if (ds != null) {
                    try {
                        ds.close();
                    } catch (SQLException e) {
                    }
                }
            }
        }
        
        public Agent getAgent() {
            return agent;
        }

        public IResourceRuntime getResource() {
            return resource;
        }

        @Override
        public int hashCode() {
            return resource.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof DbResource) {
                return resource.equals(((DbResource) obj).getResource());
            } else {
                return super.equals(obj);
            }
        }

    }
    
    class DbProvider implements IDbProvider, Serializable {
        
        private static final long serialVersionUID = 1L;
        
        List<IDb> dbs = new ArrayList<IDb>();
        
        public void refresh() {
            for (IDb db : dbs) {
                if (db instanceof DbResource) {
				    ((DbResource)db).close();
                }
            }

            dbs.clear();

            dbs.add(new IDb() {
                @Override
                public IDatabasePlatform getPlatform() {
                    return platform;
                }

                @Override
                public String getName() {
                    return "Metl DB";
                }
            });
            
            IAgentManager agentManager = context.getAgentManager();
            Collection<Agent> agents = agentManager.getAvailableAgents();
            for (Agent agent : agents) {
                AgentRuntime runtime = agentManager.getAgentRuntime(agent);
                Collection<IResourceRuntime> resources = runtime.getDeployedResources();
                for (IResourceRuntime iResource : resources) {
                    if (iResource.getResource().getType().equals(Datasource.TYPE)) {
                        DbResource db = new DbResource(agent, iResource);
                        dbs.add(db);                        
                    }
                }

            }

            Collections.sort(dbs, new Comparator<IDb>() {
                @Override
                public int compare(IDb o1, IDb o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });            
        }
        
        @Override
        public List<IDb> getDatabases() {
            return dbs;
        }
    }

}
