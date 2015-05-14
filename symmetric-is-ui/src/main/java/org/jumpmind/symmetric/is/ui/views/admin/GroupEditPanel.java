package org.jumpmind.symmetric.is.ui.views.admin;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.symmetric.is.core.model.Group;
import org.jumpmind.symmetric.is.core.model.Privilege;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class GroupEditPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;
    
    Group group;
    
    TextField nameField;
        
    public GroupEditPanel(ApplicationContext context, Group group) {
        this.context = context;
        this.group = group;

        FormLayout layout = new FormLayout();

        nameField = new TextField("Group Name", StringUtils.trimToEmpty(group.getName()));
        layout.addComponent(nameField);
        
        TwinColSelect privSelect = new TwinColSelect();
        for (Privilege priv : Privilege.values()) {
            privSelect.addItem(priv.name());
        }
        
        privSelect.setRows(6);
        privSelect.setNullSelectionAllowed(true);
        privSelect.setMultiSelect(true);
        privSelect.setImmediate(true);
        privSelect.setLeftColumnCaption("Available options");
        privSelect.setRightColumnCaption("Selected options");
        
        addComponent(layout);
        setMargin(true);
    }
    
    @Override
    public boolean closing() {
        if (!nameField.getValue().equals(group.getName())) {
            group.setName(nameField.getValue());
            context.getConfigurationService().save(group);
        }
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }
    
}
