package org.jumpmind.symmetric.is.ui.views;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
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
@TopBarLink(id="explore", category=Category.EXPLORE, menuOrder=20, name = "Explore", icon=FontAwesome.DATABASE)
public class ExploreView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    @Autowired
    IDatabasePlatform platform;
    
    public ExploreView() {
        setSizeFull();
        SqlExplorer explorer = new SqlExplorer(System.getProperty("java.io.tmpdir"),
                new IDbProvider() {
                    List<IDb> dbs;

                    @Override
                    public List<IDb> getDatabases() {
                        if (dbs == null) {
                            dbs = new ArrayList<IDb>();
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

                            // final IConfigurationService configurationService
                            // = ctx
                            // .getBean(IConfigurationService.class);
                            // final IConnectionFactory connectionFactory = ctx
                            // .getBean(IConnectionFactory.class);
                            //
                            // List<String> types = connectionFactory
                            // .getConnectionTypes(ConnectionCategory.DATASOURCE);
                            // List<Connection> connections =
                            // configurationService
                            // .findConnectionsByTypes(types.toArray(new
                            // String[types.size()]));
                            // for (final Connection connection : connections) {
                            // dbs.add(new IDb() {
                            //
                            // IDatabasePlatform platform;
                            //
                            // @Override
                            // public IDatabasePlatform getPlatform() {
                            // if (platform == null) {
                            // DataSource dataSource = connectionFactory.create(
                            // connection).reference();
                            // platform = JdbcDatabasePlatformFactory
                            // .createNewPlatformInstance(dataSource,
                            // new SqlTemplateSettings(), false);
                            // }
                            // return platform;
                            // }
                            //
                            // @Override
                            // public String getName() {
                            // return connection.getName();
                            // }
                            // });
                            // }
                        }
                        return dbs;
                    }
                }, "admin");
        addComponent(explorer);
        explorer.refresh();

    }
    @Override
    public void enter(ViewChangeEvent event) {
    }

}
