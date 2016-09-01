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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.GroupPrivilege;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.IUiPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.TwinColSelect;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class GroupEditPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;
    
    Group group;
    
    Set<String> lastPrivs;
    
    CheckBox readOnly;
    
    TwinColSelect privSelect;
        
    public GroupEditPanel(ApplicationContext context, Group group) {
        this.context = context;
        this.group = group;

        FormLayout layout = new FormLayout();

        TextField nameField = new TextField("Group Name", StringUtils.trimToEmpty(group.getName()));
        nameField.addValueChangeListener(new NameChangeListener());
        layout.addComponent(nameField);
        nameField.focus();
        
        readOnly = new CheckBox("Read Only");
        readOnly.setEnabled(isNotBlank(group.getName()));
        readOnly.setImmediate(true);
        readOnly.setValue(group.isReadOnly());
        readOnly.addValueChangeListener(new ReadOnlyChangeListener());
        layout.addComponent(readOnly);
        
        privSelect = new TwinColSelect();
        privSelect.setEnabled(isNotBlank(group.getName()));
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
            if (isNotBlank(group.getName())) {
                context.getConfigurationService().save(group);
                privSelect.setEnabled(true);
                readOnly.setEnabled(true);
            }
        }        
    }
    
    class ReadOnlyChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            group.setReadOnly((Boolean) event.getProperty().getValue());
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
