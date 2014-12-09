package org.jumpmind.symmetric.is.ui.init;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.symmetric.ui.common.AbstractSpringUI;
import org.jumpmind.symmetric.ui.sqlexplorer.IDb;
import org.jumpmind.symmetric.ui.sqlexplorer.IDbProvider;
import org.jumpmind.symmetric.ui.sqlexplorer.SqlExplorer;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.VerticalLayout;

@Theme("apptheme")
@Title("Sql Explorer")
@PreserveOnRefresh
public class SqlUI extends AbstractSpringUI {

    private static final long serialVersionUID = 1L;

    @Override
    protected void init(VaadinRequest request) {
        super.init(request);

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        SqlExplorer explorer = new SqlExplorer(System.getProperty("java.io.tmpdir"),
                new IDbProvider() {
                    List<IDb> dbs;

                    @Override
                    public List<IDb> getDatabases() {
                        if (dbs == null) {
                            dbs = new ArrayList<IDb>();
                            final WebApplicationContext ctx = getWebApplicationContext();
                            dbs.add(new IDb() {
                                @Override
                                public IDatabasePlatform getPlatform() {
                                    IDatabasePlatform platform = ctx
                                            .getBean(IDatabasePlatform.class);
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
                            // return connection.getData().getName();
                            // }
                            // });
                            // }
                        }
                        return dbs;
                    }
                }, "admin");
        content.addComponent(explorer);
        explorer.refresh();
    }

}
