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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.DbProvider;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.jumpmind.vaadin.ui.sqlexplorer.DbTree;
import org.jumpmind.vaadin.ui.sqlexplorer.DefaultSettingsProvider;

import com.vaadin.ui.Button;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class TableColumnSelectWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;

    Model model;

    DbTree dbTree;

    Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();

    TableColumnSelectListener listener;

    DbProvider provider;

    public TableColumnSelectWindow(ApplicationContext context, Model model) {
        super("Import from Database into Model");
        this.context = context;
        this.model = model;

        setWidth(600.0f, Unit.PIXELS);
        setHeight(600.0f, Unit.PIXELS);

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setSizeFull();
        layout.addComponent(new Label("Select tables and columns to import into the model."));

        Panel scrollable = new Panel();
        scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);
        scrollable.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        scrollable.setSizeFull();

        provider = new DbProvider(context);
        dbTree = new DbTree(provider, new DefaultSettingsProvider(context.getConfigDir()));
        scrollable.setContent(dbTree);

        layout.addComponent(scrollable);
        layout.setExpandRatio(scrollable, 1.0f);
        addComponent(layout, 1);

        Button refreshButton = new Button("Refresh");
        Button cancelButton = new Button("Cancel");
        Button selectButton = new Button("Import");
        addComponent(buildButtonFooter(refreshButton, cancelButton, selectButton));

        cancelButton.addClickListener(event -> close());
        selectButton.addClickListener(event -> select());
        refreshButton.addClickListener(event -> refresh());
    }

    protected void refresh() {
        provider.refresh(true);
        dbTree.refresh();
    }

    @Override
    public void attach() {
        super.attach();
        this.refresh();
    }
    
    protected void select() {
        listener.selected(getModelEntityCollection());
        close();
    }

    protected Collection<ModelEntity> getModelEntityCollection() {
        Set<Table> tables = dbTree.getSelectedTables();
        List<ModelEntity> entities = new ArrayList<>();
        for (Table table : tables) {
            ModelEntity entity = new ModelEntity();
            entity.setModelId(model.getId());
            entity.setName(table.getName());

            Column[] columns = table.getColumns();
            for (Column column : columns) {
                ModelAttrib attribute = new ModelAttrib();
                attribute.setName(column.getName());
                attribute.setPk(column.isPrimaryKey());
                try {
                    attribute.setDataType(DataType.valueOf(column.getMappedType().toUpperCase()));
                } catch (Exception ex) {
                    attribute.setDataType(DataType.OTHER);
                }
                attribute.setEntityId(entity.getId());
                entity.addModelAttribute(attribute);
            }
            
            entities.add(entity);
        }
        return entities;
    }

    public void setTableColumnSelectListener(TableColumnSelectListener listener) {
        this.listener = listener;
    }

}
