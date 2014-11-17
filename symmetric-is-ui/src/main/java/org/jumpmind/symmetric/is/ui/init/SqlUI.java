package org.jumpmind.symmetric.is.ui.init;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.symmetric.is.core.config.Connection;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.connection.ConnectionCategory;
import org.jumpmind.symmetric.is.core.runtime.connection.IConnectionFactory;
import org.jumpmind.symmetric.ui.common.AbstractSpringUI;
import org.jumpmind.symmetric.ui.sqlexplorer.IDb;
import org.jumpmind.symmetric.ui.sqlexplorer.IDbProvider;
import org.jumpmind.symmetric.ui.sqlexplorer.SqlExplorer;
import org.springframework.web.context.WebApplicationContext;

import com.vaadin.annotations.PreserveOnRefresh;
import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.server.VaadinRequest;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.VerticalLayout;

@Theme("apptheme")
@Title("Sql Explorer")
@PreserveOnRefresh
@Push(transport = Transport.WEBSOCKET)
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
                            WebApplicationContext ctx = getWebApplicationContext();
                            final IConfigurationService configurationService = ctx
                                    .getBean(IConfigurationService.class);
                            final IConnectionFactory connectionFactory = ctx
                                    .getBean(IConnectionFactory.class);
                            final DataSource configDS = ctx.getBean(DataSource.class);
                            dbs.add(new IDb() {

                                IDatabasePlatform platform;

                                @Override
                                public IDatabasePlatform getPlatform() {
                                    if (platform == null) {
                                        platform = JdbcDatabasePlatformFactory
                                                .createNewPlatformInstance(configDS,
                                                        new SqlTemplateSettings(), false);
                                    }
                                    return platform;
                                }

                                @Override
                                public String getName() {
                                    return "SymmetricIS DB";
                                }
                            });

                            List<String> types = connectionFactory
                                    .getConnectionTypes(ConnectionCategory.DATASOURCE);
                            List<Connection> connections = configurationService
                                    .findConnectionsByTypes(types.toArray(new String[types.size()]));
                            for (final Connection connection : connections) {
                                dbs.add(new IDb() {

                                    IDatabasePlatform platform;

                                    @Override
                                    public IDatabasePlatform getPlatform() {
                                        if (platform == null) {
                                            DataSource dataSource = connectionFactory.create(
                                                    connection).reference();
                                            platform = JdbcDatabasePlatformFactory
                                                    .createNewPlatformInstance(dataSource,
                                                            new SqlTemplateSettings(), false);
                                        }
                                        return platform;
                                    }

                                    @Override
                                    public String getName() {
                                        return connection.getData().getName();
                                    }
                                });
                            }
                        }
                        return dbs;
                    }
                }, "admin");
        content.addComponent(explorer);
        explorer.refresh();
    }

}
