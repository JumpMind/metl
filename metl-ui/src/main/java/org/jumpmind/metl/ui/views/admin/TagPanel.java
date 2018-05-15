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

import org.jumpmind.metl.core.model.Tag;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.vaadin.ui.common.IUiPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class TagPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;
    
    TabbedPanel tabbedPanel;
    
    Button newButton;
    
    Button editButton;
    
    Button removeButton;

    BeanItemContainer<Tag> container;
    
    Table table;
    
    public TagPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;
        
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        newButton = buttonBar.addButton("New", FontAwesome.PLUS);
        newButton.addClickListener(new NewClickListener());

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new RemoveClickListener());

        container = new BeanItemContainer<Tag>(Tag.class);

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns("name", "color");
        table.setColumnHeaders("Tag Name", "Color");
        table.addItemClickListener(new TableItemClickListener());
        table.addValueChangeListener(new TableValueChangeListener());
        table.setSortContainerPropertyId("name");
        table.setSortAscending(true);

        addComponent(table);
        setExpandRatio(table, 1.0f);
        refresh();
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
        container.addAll(context.getConfigurationService().findTags());
        table.sort();
        setButtonsEnabled();
    }

    protected void setButtonsEnabled() {
        Set<Tag> selectedIds = getSelectedItems();
        boolean enabled = selectedIds.size() > 0;
        editButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    @SuppressWarnings("unchecked")
    protected Set<Tag> getSelectedItems() {
        return (Set<Tag>) table.getValue();
    }

    @SuppressWarnings("unchecked")
    protected Tag getFirstSelectedItem() {
        Set<Tag> tags = (Set<Tag>) table.getValue();
        Iterator<Tag> iter = tags.iterator();
        if (iter.hasNext()) {
            return iter.next();
        }
        return null;
    }

    class NewClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Tag tag = new Tag();
            TagEditPanel editPanel = new TagEditPanel(context, tag);
            tabbedPanel.addCloseableTab(tag.getId(), "Edit Tag", getIcon(), editPanel);
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Tag tag = getFirstSelectedItem();
//TODO: refresh if we want to do things like show all entities that are tagged            
//            context.getOperationsService().refresh(tag);
            TagEditPanel editPanel = new TagEditPanel(context, tag);
            tabbedPanel.addCloseableTab(tag.getId(), "Edit Tag", getIcon(), editPanel);
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            IConfigurationService configurationService = context.getConfigurationService();
            for (Tag tag : getSelectedItems()) {
                configurationService.deleteEntityTagsForTag(tag);
                configurationService.delete(tag);                
                container.removeItem(tag);
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
}
