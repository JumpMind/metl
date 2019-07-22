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
package org.jumpmind.metl.ui.views.admin;

import java.util.Iterator;
import java.util.Set;

import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UIConstants;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.core.annotation.Order;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.BeanItemContainer;
import com.vaadin.v7.event.ItemClickEvent;
import com.vaadin.v7.event.ItemClickEvent.ItemClickListener;
import com.vaadin.v7.ui.Table;

@SuppressWarnings("serial")
@UiComponent
@UIScope
@Order(600)
@AdminMenuLink(name = "Plugin Repositories", id = "Plugin Repositories", icon = FontAwesome.DATABASE)
public class PluginRepositoriesPanel extends AbstractAdminPanel {

    Button newButton;
    
    Button editButton;
    
    Button removeButton;

    BeanItemContainer<PluginRepository> container;
    
    Table table;
    
    public PluginRepositoriesPanel() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        newButton = buttonBar.addButton("Add", FontAwesome.PLUS);
        newButton.addClickListener(new NewClickListener());

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new RemoveClickListener());

        container = new BeanItemContainer<PluginRepository>(PluginRepository.class);

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns("name", "url", "lastUpdateTime");
        table.setColumnHeaders("Name", "Url", "Updated");
        table.setColumnWidth("lastUpdateTime", UIConstants.DATETIME_WIDTH_PIXELS);
        table.addItemClickListener(new TableItemClickListener());
        table.addValueChangeListener(new TableValueChangeListener());
        table.setSortContainerPropertyId("name");
        table.setSortAscending(true);

        addComponent(table);
        setExpandRatio(table, 1.0f);
    }

    @Override
    public void selected() {
        refresh();
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {        
    }

    public void refresh() {
        container.removeAllItems();
        container.addAll(context.getPluginService().findPluginRepositories());
        table.sort();
        setButtonsEnabled();
    }

    protected void setButtonsEnabled() {
        Set<PluginRepository> selectedIds = getSelectedItems();
        boolean enabled = selectedIds.size() > 0;
        editButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    @SuppressWarnings("unchecked")
    protected Set<PluginRepository> getSelectedItems() {
        return (Set<PluginRepository>) table.getValue();
    }

    @SuppressWarnings("unchecked")
    protected PluginRepository getFirstSelectedItem() {
        Set<PluginRepository> pluginRepositorys = (Set<PluginRepository>) table.getValue();
        Iterator<PluginRepository> iter = pluginRepositorys.iterator();
        if (iter.hasNext()) {
            return iter.next();
        }
        return null;
    }

    class NewClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            PluginRepository pluginRepository = new PluginRepository();
            PluginRepositoryEditPanel editPanel = new PluginRepositoryEditPanel(context, pluginRepository);
            adminView.getTabbedPanel().addCloseableTab(pluginRepository.getId(), "Edit Repository", getIcon(), editPanel);
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            PluginRepository pluginRepository = getFirstSelectedItem();
            context.getPluginService().refresh(pluginRepository);
            PluginRepositoryEditPanel editPanel = new PluginRepositoryEditPanel(context, pluginRepository);
            adminView.getTabbedPanel().addCloseableTab(pluginRepository.getId(), "Edit Repository", getIcon(), editPanel);
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            for (PluginRepository pluginRepository : getSelectedItems()) {
                context.getConfigurationService().delete(pluginRepository);
                container.removeItem(pluginRepository);
            }
            table.setValue(null);
            setButtonsEnabled();
        }
    }

    class TableItemClickListener implements ItemClickListener {
        long lastClick;
        
        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                editButton.click();
            } else if (getSelectedItems().contains(event.getItemId()) &&
                System.currentTimeMillis()-lastClick > 500) {
                    table.setValue(null);
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TableValueChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            setButtonsEnabled();
        }
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }
}
