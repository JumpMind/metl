package org.jumpmind.symmetric.is.ui.views.design;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourcePropertyConstants;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Resource;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.component.DbReader;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.sqlexplorer.IButtonBar;
import org.jumpmind.symmetric.ui.sqlexplorer.IDb;
import org.jumpmind.symmetric.ui.sqlexplorer.ISettingsProvider;
import org.jumpmind.symmetric.ui.sqlexplorer.QueryPanel;
import org.jumpmind.symmetric.ui.sqlexplorer.Settings;

import com.vaadin.event.ShortcutListener;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.event.ShortcutAction.ModifierKey;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditDbReaderPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Component component;

    IDatabasePlatform platform;

    QueryPanel queryPanel;

    Button executeButton;

    ExecuteSqlClickListener executeSqlClickListener;
    
    PropertySheet propertySheet;

    public EditDbReaderPanel(ApplicationContext context, Component component, PropertySheet propertySheet) {
        this.context = context;
        this.component = component;
        this.propertySheet = propertySheet;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        executeSqlClickListener = new ExecuteSqlClickListener();

        executeButton = buttonBar.addButton("Execute", FontAwesome.PLAY, executeSqlClickListener);
        executeButton.setEnabled(false);

        Resource resource = component.getResource();

        if (resource != null) {
            resource.put(BasicDataSourcePropertyConstants.DB_POOL_INITIAL_SIZE, "2");
            resource.put(BasicDataSourcePropertyConstants.DB_POOL_MAX_ACTIVE, "2");
            resource.put(BasicDataSourcePropertyConstants.DB_POOL_MAX_IDLE, "2");
            resource.put(BasicDataSourcePropertyConstants.DB_POOL_MIN_IDLE, "2");
            DataSourceResource dataSourceResource = (DataSourceResource) context
                    .getResourceFactory().create(resource, null);
            DataSource dataSource = dataSourceResource.reference();
            platform = JdbcDatabasePlatformFactory.createNewPlatformInstance(dataSource,
                    new SqlTemplateSettings(), false);

            queryPanel = new QueryPanel(new IDb() {

                @Override
                public IDatabasePlatform getPlatform() {
                    return platform;
                }

                @Override
                public String getName() {
                    return "";
                }
            }, new ISettingsProvider() {

                Settings settings = new Settings();

                @Override
                public void save(Settings settings) {
                }

                @Override
                public Settings load() {
                    return settings;
                }

                @Override
                public Settings get() {
                    return settings;
                }
            }, new IButtonBar() {

                @Override
                public void setRollbackButtonEnabled(boolean enabled) {
                }

                @Override
                public void setExecuteScriptButtonEnabled(boolean enabled) {
                }

                @Override
                public void setExecuteAtCursorButtonEnabled(boolean enabled) {
                    executeButton.setEnabled(enabled);
                }

                @Override
                public void setCommitButtonEnabled(boolean enabled) {
                }
            }, context.getUser().getLoginId());

            queryPanel.appendSql(component.get(DbReader.SQL));

            queryPanel.addShortcutListener(new ShortcutListener("", KeyCode.ENTER,
                    new int[] { ModifierKey.CTRL }) {
                @Override
                public void handleAction(Object sender, Object target) {
                    executeSqlClickListener.buttonClick(new ClickEvent(executeButton));
                }
            }

            );

            addComponent(queryPanel);
            setExpandRatio(queryPanel, 1);

        } else {
            Label label = new Label("Before configuring SQL you must select a data source");
            addComponent(label);
            setExpandRatio(label, 1);
        }

    }

    protected void save() {
        Setting data = component.findSetting(DbReader.SQL);
        data.setValue(queryPanel.getSql());
        context.getConfigurationService().save(data);
    }

    @Override
    public boolean closing() {
        if (queryPanel != null) {
            save();
            BasicDataSource dataSource = (BasicDataSource) platform.getDataSource();
            try {
                dataSource.close();
            } catch (SQLException e) {
            }
        }
        if (propertySheet != null) {
            propertySheet.valueChange(component);
        }
        return true;
    }

    @Override
    public void selected() {
        if (queryPanel != null) {
            queryPanel.selected();
        }
    }
    
    @Override
    public void deselected() {
    }

    class ExecuteSqlClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            save();
            queryPanel.execute(false);
        }
    }

}
