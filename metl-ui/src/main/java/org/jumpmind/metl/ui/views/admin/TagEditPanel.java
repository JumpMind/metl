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

import java.awt.Color;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.Tag;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.vaadin.addons.tatu.ColorPicker;
import org.vaadin.addons.tatu.ColorPickerVariant;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.Command;

@SuppressWarnings("serial")

public class TagEditPanel extends VerticalLayout implements IUiPanel {

    protected static final String NOCHANGE = "******";

    ApplicationContext context;

    Tag tag;
    
    Command refreshGridCommand;

    Map<String, Group> groupsById;

    Set<String> lastGroups;

    public TagEditPanel(ApplicationContext context, Tag tag, Command refreshGridCommand) {
        this.context = context;
        this.tag = tag;
        this.refreshGridCommand = refreshGridCommand;

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new ResponsiveStep("0", 1));
        
        TextField nameField = new TextField();
        nameField.setValue(StringUtils.trimToEmpty(tag.getName()));
        form.addFormItem(nameField, "Name");
        nameField.addValueChangeListener(new NameChangeListener());
        nameField.focus();

        ColorPicker colorPickerField = new ColorPicker();
        colorPickerField.addThemeVariants(ColorPickerVariant.COMPACT);
        colorPickerField.setValue(String.format("#%06x", 0xFFFFFF & tag.getColor()));
        colorPickerField.addValueChangeListener(new ColorFieldListener());         
                
        form.addFormItem(colorPickerField, "Color");
        
//        List<Group> groups = context.getOperationsService().findGroups();
//        groupsById = new HashMap<String, Group>();
//        TwinColSelect groupSelect = new TwinColSelect();
//        for (Group group : groups) {
//            groupSelect.addItem(group.getId());
//            groupSelect.setItemCaption(group.getId(), group.getName());
//            groupsById.put(group.getId(), group);
//        }
//        lastGroups = new HashSet<String>();
//        for (Group group : user.getGroups()) {
//            lastGroups.add(group.getId());
//        }
//        groupSelect.setValue(lastGroups);
//        groupSelect.setRows(20);
//        groupSelect.setNullSelectionAllowed(true);
//        groupSelect.setMultiSelect(true);
//        groupSelect.setImmediate(true);
//        groupSelect.setLeftColumnCaption("Available groups");
//        groupSelect.setRightColumnCaption("Selected groups");
//        groupSelect.addValueChangeListener(new GroupChangeListener());
//        form.addComponent(groupSelect);

        add(form);
        setMargin(true);
    }

    @Override
    public boolean closing() {
        refreshGridCommand.execute();
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    protected void save(Tag tag) {
        if (isNotBlank(tag.getName())) {
            context.getConfigurationService().save(tag);
        }
    }

    class NameChangeListener implements ValueChangeListener<ValueChangeEvent<String>> {
        public void valueChanged(ValueChangeEvent<String> event) {
            tag.setName(event.getValue());
            save(tag);
        }
    }

    class ColorFieldListener implements ValueChangeListener<ValueChangeEvent<String>> {
        @Override
        public void valueChanged(ValueChangeEvent<String> event) {
            tag.setColor(Color.decode(event.getValue()).getRGB());
            save(tag);
        }
    }    
    
//    class GroupChangeListener implements ValueChangeListener {
//        @SuppressWarnings("unchecked")
//        public void valueChange(ValueChangeEvent event) {
//            Set<String> groups = (Set<String>) event.getProperty().getValue();
//            IOperationsService operationsSerivce = context.getOperationsService();
//            for (String id : groups) {
//                if (!lastGroups.contains(id)) {
//                    UserGroup userGroup = new UserGroup(user.getId(), id);
//                    user.getGroups().add(groupsById.get(id));
//                    if (operationsSerivce.findUser(user.getId()) != null) {
//                        context.getConfigurationService().save(userGroup);
//                    }
//                }
//            }
//
//            for (String id : lastGroups) {
//                if (!groups.contains(id)) {
//                    user.getGroups().remove(groupsById.get(id));
//                    if (operationsSerivce.findUser(user.getId()) != null) {
//                        context.getConfigurationService().delete(new UserGroup(user.getId(), id));
//                    }
//                }
//            }
//
//            lastGroups = new HashSet<String>(groups);
//        }
//    }

}
