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

import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.Group;
import org.jumpmind.metl.core.model.Tag;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.IUiPanel;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.shared.ui.colorpicker.Color;
import com.vaadin.ui.ColorPicker;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.colorpicker.ColorChangeEvent;
import com.vaadin.ui.components.colorpicker.ColorChangeListener;

@SuppressWarnings("serial")

public class TagEditPanel extends VerticalLayout implements IUiPanel {

    protected static final String NOCHANGE = "******";

    ApplicationContext context;

    Tag tag;

    Map<String, Group> groupsById;

    Set<String> lastGroups;

    @SuppressWarnings("deprecation")
    public TagEditPanel(ApplicationContext context, Tag tag) {
        this.context = context;
        this.tag = tag;

        FormLayout form = new FormLayout();
        form.setSpacing(true);
        
        TextField nameField = new TextField("Name", StringUtils.trimToEmpty(tag.getName()));
        form.addComponent(nameField);
        nameField.addValueChangeListener(new NameChangeListener());
        nameField.focus();

        ColorPicker colorPickerField = new ColorPicker("Tag Color");
        colorPickerField.setSwatchesVisibility(true);
        colorPickerField.setHistoryVisibility(true);
        colorPickerField.setTextfieldVisibility(false);
        colorPickerField.setHSVVisibility(false);
        colorPickerField.setColor(new Color(tag.getColor()));
        colorPickerField.addColorChangeListener(new ColorFieldListener());         

        HorizontalLayout hLayout = new HorizontalLayout();
        hLayout.setCaption("Color");
        hLayout.addComponent(colorPickerField);
                
        form.addComponent(hLayout);
        
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

        addComponent(form);
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

    protected void save(Tag tag) {
        if (isNotBlank(tag.getName())) {
            context.getConfigurationService().save(tag);
        }
    }

    class NameChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            tag.setName((String) event.getProperty().getValue());
            save(tag);
        }
    }

    class ColorFieldListener implements ColorChangeListener {
        @Override
        public void colorChanged(ColorChangeEvent event) {
            tag.setColor(event.getColor().getRGB());
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
