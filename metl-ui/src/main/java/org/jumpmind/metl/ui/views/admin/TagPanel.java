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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.Tag;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.springframework.context.annotation.Scope;
import org.springframework.core.annotation.Order;

import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.ItemClick;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.components.grid.ItemClickListener;

@SuppressWarnings("serial")
@UiComponent
@Scope(value = "ui")
@Order(300)
@AdminMenuLink(name = "Tags", id = "Tags", icon = VaadinIcons.TAG)
public class TagPanel extends AbstractAdminPanel {

    Button newButton;
    
    Button editButton;
    
    Button removeButton;
    
    List<Tag> tagList = new ArrayList<Tag>();
    
    Grid<Tag> grid;
    
    public TagPanel() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        newButton = buttonBar.addButton("New", VaadinIcons.PLUS);
        newButton.addClickListener(new NewClickListener());

        editButton = buttonBar.addButton("Edit", VaadinIcons.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", VaadinIcons.TRASH);
        removeButton.addClickListener(new RemoveClickListener());

        grid = new Grid<Tag>();
        grid.setSizeFull();
        //grid.setCacheRate(100);
        //grid.setPageLength(100);
        grid.setSelectionMode(SelectionMode.MULTI);

        grid.addColumn(Tag::getName).setId("name").setCaption("Tag Name").setSortable(true);
        grid.addColumn(Tag::getColor).setCaption("Color");
        grid.addItemClickListener(new GridItemClickListener());
        grid.addSelectionListener(new GridSelectionListener());
        grid.sort("name", SortDirection.ASCENDING);

        addComponent(grid);
        setExpandRatio(grid, 1.0f);
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
        tagList.clear();
        tagList.addAll(context.getConfigurationService().findTags());
        tagList.sort(null);
        grid.setItems(tagList);
        setButtonsEnabled();
    }

    protected void setButtonsEnabled() {
        Set<Tag> selectedIds = getSelectedItems();
        boolean enabled = selectedIds.size() > 0;
        editButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    protected Set<Tag> getSelectedItems() {
        return grid.getSelectedItems();
    }

    protected Tag getFirstSelectedItem() {
        Set<Tag> tags = grid.getSelectedItems();
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
            adminView.getTabbedPanel().addCloseableTab(tag.getId(), "Edit Tag", getIcon(), editPanel);
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Tag tag = getFirstSelectedItem();
//TODO: refresh if we want to do things like show all entities that are tagged            
//            context.getOperationsService().refresh(tag);
            TagEditPanel editPanel = new TagEditPanel(context, tag);
            adminView.getTabbedPanel().addCloseableTab(tag.getId(), "Edit Tag", getIcon(), editPanel);
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            IConfigurationService configurationService = context.getConfigurationService();
            for (Tag tag : getSelectedItems()) {
                configurationService.deleteEntityTagsForTag(tag);
                configurationService.delete(tag);                
                tagList.remove(tag);
            }
            grid.setItems(tagList);
            grid.deselectAll();
            setButtonsEnabled();
        }
    }

    class GridItemClickListener implements ItemClickListener<Tag> {
        long lastClick;
        
        public void itemClick(ItemClick<Tag> event) {
            if (event.getMouseEventDetails().isDoubleClick()) {
                editButton.click();
            } else if (getSelectedItems().contains(event.getItem()) &&
                System.currentTimeMillis()-lastClick > 500) {
                    grid.deselectAll();
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class GridSelectionListener implements SelectionListener<Tag> {
        public void selectionChange(SelectionEvent<Tag> event) {
            setButtonsEnabled();
        }
    }
    
    @Override
    public void enter(ViewChangeEvent event) {
    }

}
