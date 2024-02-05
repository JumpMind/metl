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
package org.jumpmind.metl.ui.views.design;

import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.BasicDataSource;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.db.platform.JdbcDatabasePlatformFactory;
import org.jumpmind.db.sql.SqlTemplateSettings;
import org.jumpmind.db.util.BasicDataSourcePropertyConstants;
import org.jumpmind.metl.core.model.Resource;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.RdbmsReader;
import org.jumpmind.metl.core.runtime.resource.Datasource;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.sqlexplorer.IButtonBar;
import org.jumpmind.vaadin.ui.sqlexplorer.IDb;
import org.jumpmind.vaadin.ui.sqlexplorer.ISettingsProvider;
import org.jumpmind.vaadin.ui.sqlexplorer.QueryPanel;
import org.jumpmind.vaadin.ui.sqlexplorer.Settings;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;

public class EditRdbmsReaderPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;

    IDatabasePlatform platform;

    QueryPanel queryPanel;

    Button executeButton;

    ExecuteSqlClickListener executeSqlClickListener;

    protected void buildUI() {
        if (!readOnly) {
            ButtonBar buttonBar = new ButtonBar();
            add(buttonBar);

            executeSqlClickListener = new ExecuteSqlClickListener();

            executeButton = buttonBar.addButton("Execute", VaadinIcon.PLAY, executeSqlClickListener);
            executeButton.setEnabled(false);

            Resource resource = component.getResource();

            if (resource != null) {
                resource.put(BasicDataSourcePropertyConstants.DB_POOL_INITIAL_SIZE, "2");
                resource.put(BasicDataSourcePropertyConstants.DB_POOL_MAX_ACTIVE, "2");
                resource.put(BasicDataSourcePropertyConstants.DB_POOL_MAX_IDLE, "2");
                resource.put(BasicDataSourcePropertyConstants.DB_POOL_MIN_IDLE, "2");
                Datasource dataSourceResource = new Datasource();
                dataSourceResource.start(resource, resource.toTypedProperties(context.getDefinitionFactory()
                        .getResourceDefintion(component.getProjectVersionId(), Datasource.TYPE).getSettings().getSetting()));
                DataSource dataSource = dataSourceResource.reference();
                platform = JdbcDatabasePlatformFactory.getInstance().create(dataSource, new SqlTemplateSettings(), false, false);

                queryPanel = new QueryPanel(new IDb() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public IDatabasePlatform getPlatform() {
                        return platform;
                    }

                    @Override
                    public String getName() {
                        return "";
                    }
                }, new ISettingsProvider() {

                    private static final long serialVersionUID = 1L;

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

                    private static final long serialVersionUID = 1L;

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

                queryPanel.appendSql(component.get(RdbmsReader.SQL));

                Shortcuts.addShortcutListener(queryPanel, () -> {
                    executeSqlClickListener.onComponentEvent(new ClickEvent<Button>(executeButton));
                }, Key.ENTER, KeyModifier.CONTROL);

                add(queryPanel);
                expand(queryPanel);
            } else {
                Span span = new Span("Before configuring SQL you must select a data source");
                add(span);
                expand(span);
            }
        } else {
            AceEditor editor = CommonUiUtils.createAceEditor();
            editor.setMode(AceMode.sql);
            editor.setValue(component.get(RdbmsReader.SQL));
            editor.setReadOnly(readOnly);
            add(editor);
            expand(editor);
        }
    }

    protected void save() {
        Setting data = component.findSetting(RdbmsReader.SQL);
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
            propertySheet.setSource(component);
        }
        return true;
    }

    @Override
    public void selected() {
        if (queryPanel != null) {
            queryPanel.selected();
        }
    }

    class ExecuteSqlClickListener implements ComponentEventListener<ClickEvent<Button>> {
        private static final long serialVersionUID = 1L;

        public void onComponentEvent(ClickEvent<Button> event) {
            save();
            queryPanel.execute(false);
        }
    }

}
