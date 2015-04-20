package org.jumpmind.symmetric.is.ui.views;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourcePropertyConstants;
import org.jumpmind.symmetric.is.core.model.ProjectVersion;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.AppConstants;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.Category;
import org.jumpmind.symmetric.is.ui.common.TopBarLink;
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

    class DbResource implements IDb {

        Resource resource;

        ProjectVersion projectVersion;

        IDatabasePlatform platform;

        public DbResource(ProjectVersion projectVersion, Resource resource) {
            this.resource = resource;
            this.projectVersion = projectVersion;
        }

        @Override
        public String getName() {
            return projectVersion.getName() + " " + resource.getName();
        }

        @Override
        public IDatabasePlatform getPlatform() {
            if (platform == null) {
                resource.put(BasicDataSourcePropertyConstants.DB_POOL_INITIAL_SIZE, "2");
                resource.put(BasicDataSourcePropertyConstants.DB_POOL_MAX_ACTIVE, "2");
                resource.put(BasicDataSourcePropertyConstants.DB_POOL_MAX_IDLE, "2");
                resource.put(BasicDataSourcePropertyConstants.DB_POOL_MIN_IDLE, "2");
                DataSourceResource dataSourceResource = (DataSourceResource) context
                        .getResourceFactory().create(resource, null);
                DataSource dataSource = dataSourceResource.reference();
                platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource,
                        new SqlTemplateSettings(), false);
            }
            return platform;
        }

        public ProjectVersion getProjectVersion() {
            return projectVersion;
        }

        public Resource getResource() {
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
    
    class DbProvider implements IDbProvider {
        
        List<IDb> dbs = new ArrayList<IDb>();
        
        public void refresh() {
            for (IDb db : dbs) {
                BasicDataSource ds = db.getPlatform().getDataSource();
                try {
                    ds.close();
                } catch (SQLException e) {
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
                    return "SymmetricIS DB";
                }
            });

            List<ProjectVersion> projects = context.getOpenProjects();
            for (ProjectVersion projectVersion : projects) {
                List<Resource> resources = context.getConfigurationService()
                        .findResourcesInProject(projectVersion.getId());
                for (Resource resource : resources) {
                    if (resource.getType().equals(DataSourceResource.TYPE)) {
                        DbResource db = new DbResource(projectVersion, resource);
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
