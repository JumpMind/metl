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

import org.apache.commons.lang3.StringUtils;
import org.jumpmind.metl.core.model.Notification;
import org.jumpmind.metl.core.model.User;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.vaadin.ui.common.IUiPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class NotificationPanel extends VerticalLayout implements IUiPanel, TextChangeListener {

    ApplicationContext context;
    
    TabbedPanel tabbedPanel;

    Button newButton;
    
    Button editButton;
    
    Button removeButton;

    BeanItemContainer<Notification> container;
    
    Table table;
    
    TextField filterField;

    public NotificationPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
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

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(this);
        
        container = new BeanItemContainer<Notification>(Notification.class);

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns("notificationLevel", "name", "notifyType", "eventType", "enabled");
        table.setColumnHeaders("Level", "Name", "Notify Type", "Event Type", "Enabled");
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

    @Override
    public void textChange(TextChangeEvent event) {
        refresh();
    }

    public void refresh() {
        container.removeAllItems();
        if (StringUtils.isEmpty(filterField.getValue())) {
            container.addAll(context.getOperationsService().findNotifications());
        } else {
            String filter = filterField.getValue().toUpperCase();
            for (Notification notification : context.getOperationsService().findNotifications()) {
                if (notification.getNotificationLevel().indexOf(filter) != -1 || notification.getName().indexOf(filter) != -1 ||
                    notification.getEventType().indexOf(filter) != -1) {
                    container.addItem(notification);
                }
            }
        }
        table.sort();
        setButtonsEnabled();
    }

    protected void setButtonsEnabled() {
        Set<Notification> selectedIds = getSelectedItems();
        boolean enabled = selectedIds.size() > 0;
        editButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    @SuppressWarnings("unchecked")
    protected Set<Notification> getSelectedItems() {
        return (Set<Notification>) table.getValue();
    }

    @SuppressWarnings("unchecked")
    protected Notification getFirstSelectedItem() {
        Set<Notification> items = (Set<Notification>) table.getValue();
        Iterator<Notification> iter = items.iterator();
        if (iter.hasNext()) {
            return iter.next();
        }
        return null;
    }

    class NewClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            User user = new User();
            Notification notification = new Notification();
            NotificationEditPanel editPanel = new NotificationEditPanel(context, notification); 
            tabbedPanel.addCloseableTab(user.getId(), "Edit Notification", getIcon(), editPanel);
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Notification item = getFirstSelectedItem();
            context.getOperationsService().refresh(item);
            NotificationEditPanel editPanel = new NotificationEditPanel(context, item);
            tabbedPanel.addCloseableTab(item.getId(), "Edit Notification", getIcon(), editPanel);
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            for (Notification item : getSelectedItems()) {
                context.getConfigurationService().delete(item);
                container.removeItem(item);
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
