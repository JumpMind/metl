package org.jumpmind.metl.ui.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.resource.Datasource;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.symmetric.ui.sqlexplorer.IDb;
import org.jumpmind.symmetric.ui.sqlexplorer.IDbProvider;

public class DbProvider implements IDbProvider, Serializable {

    private static final long serialVersionUID = 1L;

    List<IDb> dbs = new ArrayList<IDb>();

    ApplicationContext context;

    public DbProvider(ApplicationContext context) {
        this.context = context;
    }

    public void refresh() {
        for (IDb db : dbs) {
            if (db instanceof DbResource) {
                ((DbResource) db).close();
            }
        }

        dbs.clear();

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

        dbs.add(0, new IDb() {
            @Override
            public IDatabasePlatform getPlatform() {
                return context.getConfigDatabasePlatform();
            }

            @Override
            public String getName() {
                return "Metl DB";
            }
        });

    }

    @Override
    public List<IDb> getDatabases() {
        return dbs;
    }
}