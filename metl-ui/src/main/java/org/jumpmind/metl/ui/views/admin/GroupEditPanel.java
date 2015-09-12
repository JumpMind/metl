package org.jumpmind.metl.ui.views.admin;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.GroupPrivilege;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class GroupEditPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;
    
    Group group;
    
    Set<String> lastPrivs;
        
    public GroupEditPanel(ApplicationContext context, Group group) {
        this.context = context;
        this.group = group;

        FormLayout layout = new FormLayout();

        TextField nameField = new TextField("Group Name", StringUtils.trimToEmpty(group.getName()));
        nameField.addValueChangeListener(new NameChangeListener());
        layout.addComponent(nameField);
        nameField.focus();
        
        TwinColSelect privSelect = new TwinColSelect();
        for (Privilege priv : Privilege.values()) {
            privSelect.addItem(priv.name());
        }
        lastPrivs = new HashSet<String>();
        for (GroupPrivilege groupPriv : group.getGroupPrivileges()) {
            lastPrivs.add(groupPriv.getName());
        }
        privSelect.setValue(lastPrivs);
        privSelect.setRows(20);
        privSelect.setNullSelectionAllowed(true);
        privSelect.setMultiSelect(true);
        privSelect.setImmediate(true);
        privSelect.setLeftColumnCaption("Available privileges");
        privSelect.setRightColumnCaption("Selected privileges");
        privSelect.addValueChangeListener(new PrivilegeChangeListener());
        layout.addComponent(privSelect);

        addComponent(layout);
        setMargin(true);
    }
    
    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    class NameChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            group.setName((String) event.getProperty().getValue());
            context.getConfigurationService().save(group);
        }        
    }
    
    class PrivilegeChangeListener implements ValueChangeListener {
        @SuppressWarnings("unchecked")
        public void valueChange(ValueChangeEvent event) {
            Set<String> privs = (Set<String>) event.getProperty().getValue();
            
            for (String name : privs) {
                if (!lastPrivs.contains(name)) {
                    GroupPrivilege groupPriv = new GroupPrivilege(group.getId(), name);
                    group.getGroupPrivileges().add(groupPriv);
                    context.getConfigurationService().save(groupPriv);
                }
            }

            for (String name : lastPrivs) {
                if (!privs.contains(name)) {
                    Iterator<GroupPrivilege> iter = group.getGroupPrivileges().iterator();
                    while (iter.hasNext()) {
                        GroupPrivilege groupPriv = iter.next();
                        if (groupPriv.getName().equals(name)) {
                            iter.remove();
                            context.getConfigurationService().delete(groupPriv);
                        }
                    }
                }
            }
            
            lastPrivs = new HashSet<String>(privs);
        }
    }
    
}
