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
import org.springframework.core.annotation.Order;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.grid.ItemClickEvent;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.spring.annotation.UIScope;

@SuppressWarnings("serial")
@UiComponent
@UIScope
@Order(300)
@AdminMenuLink(name = "Tags", id = "Tags", icon = VaadinIcon.TAG)
public class TagPanel extends AbstractAdminPanel {

    Button newButton;
    
    Button editButton;
    
    Button removeButton;
    
    List<Tag> tagList = new ArrayList<Tag>();
    
    Grid<Tag> grid;
    
    public TagPanel() {
        setPadding(false);
        setSpacing(false);
        
        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);

        newButton = buttonBar.addButton("New", VaadinIcon.PLUS);
        newButton.addClickListener(new NewClickListener());

        editButton = buttonBar.addButton("Edit", VaadinIcon.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", VaadinIcon.TRASH);
        removeButton.addClickListener(new RemoveClickListener());

        grid = new Grid<Tag>();
        grid.setSizeFull();
        grid.setPageSize(100);
        grid.setSelectionMode(SelectionMode.MULTI);

        grid.addColumn(Tag::getName).setKey("name").setHeader("Tag Name").setSortable(true);
        grid.addColumn(Tag::getColor).setHeader("Color");
        grid.addItemClickListener(new GridItemClickListener());
        grid.addSelectionListener(new GridSelectionListener());
        List<GridSortOrder<Tag>> orderList = new ArrayList<GridSortOrder<Tag>>();
        orderList.add(new GridSortOrder<Tag>(grid.getColumnByKey("name"), SortDirection.ASCENDING));
        grid.sort(orderList);

        add(grid);
        expand(grid);
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

    class NewClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Tag tag = new Tag();
            TagEditPanel editPanel = new TagEditPanel(context, tag, () -> refresh());
            adminView.getTabbedPanel().addCloseableTab(tag.getId(), "Edit Tag", new Icon(VaadinIcon.TAG), editPanel);
        }
    }

    class EditClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            Tag tag = getFirstSelectedItem();
//TODO: refresh if we want to do things like show all entities that are tagged            
//            context.getOperationsService().refresh(tag);
            TagEditPanel editPanel = new TagEditPanel(context, tag, () -> refresh());
            adminView.getTabbedPanel().addCloseableTab(tag.getId(), "Edit Tag", new Icon(VaadinIcon.TAG), editPanel);
        }
    }

    class RemoveClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
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

    class GridItemClickListener implements ComponentEventListener<ItemClickEvent<Tag>> {
        long lastClick;
        
        public void onComponentEvent(ItemClickEvent<Tag> event) {
            if (event.getClickCount() == 2) {
                editButton.click();
            } else if (getSelectedItems().contains(event.getItem()) &&
                System.currentTimeMillis()-lastClick > 500) {
                    grid.deselectAll();
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class GridSelectionListener implements SelectionListener<Grid<Tag>, Tag> {
        public void selectionChange(SelectionEvent<Grid<Tag>, Tag> event) {
            setButtonsEnabled();
        }
    }

}
