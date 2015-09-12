package org.jumpmind.symmetric.is.ui.views.admin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.Group;
import org.jumpmind.symmetric.is.core.model.User;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Notification;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class GroupPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    Button newButton;
    
    Button editButton;
    
    Button removeButton;

    BeanItemContainer<Group> container;
    
    Table table;

    public GroupPanel(ApplicationContext context, TabbedPanel tabbedPanel) {
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

        container = new BeanItemContainer<Group>(Group.class);

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns("name", "createTime", "lastUpdateTime");
        table.setColumnHeaders("Name", "Create Time", "Update Time");
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
        container.addAll(context.getConfigurationService().findGroups());
        table.sort();
        setButtonsEnabled();
    }

    protected void setButtonsEnabled() {
        Set<Group> selectedIds = getSelectedItems();
        boolean enabled = selectedIds.size() > 0;
        editButton.setEnabled(enabled);
        removeButton.setEnabled(enabled);
    }

    @SuppressWarnings("unchecked")
    protected Set<Group> getSelectedItems() {
        return (Set<Group>) table.getValue();
    }

    @SuppressWarnings("unchecked")
    protected Group getFirstSelectedItem() {
        Set<Group> groups = (Set<Group>) table.getValue();
        Iterator<Group> iter = groups.iterator();
        if (iter.hasNext()) {
            return iter.next();
        }
        return null;
    }

    class NewClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Group group = new Group();
            GroupEditPanel editPanel = new GroupEditPanel(context, group);
            tabbedPanel.addCloseableTab(group.getId(), "Edit Group", getIcon(), editPanel);
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Group group = getFirstSelectedItem();
            context.getConfigurationService().refresh(group);
            GroupEditPanel editPanel = new GroupEditPanel(context, group);
            tabbedPanel.addCloseableTab(group.getId(), "Edit Group", getIcon(), editPanel);
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            List<User> users = new ArrayList<User>();
            for (Group group : getSelectedItems()) {
                users.addAll(context.getConfigurationService().findUsersByGroup(group.getId()));
                if (users.size() > 10) {
                    break;
                }
            }
            if (users.size() == 0) {
                for (Group group : getSelectedItems()) {
                    context.getConfigurationService().delete(group);
                    container.removeItem(group);
                }
            } else {
                String message = "There are " + users.size() + " users assigned to this group. " +
                        "Re-assign or delete the users first.  ";
                if (users.size() < 10) {
                    message += users.toString();
                }
                Notification note = new Notification("Cannot Delete", message);
                note.show(Page.getCurrent());
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
