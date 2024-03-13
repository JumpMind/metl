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
package org.jumpmind.metl.ui.views.design;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.ResizableDialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.ValueChangeMode;

@SuppressWarnings("serial")
class EditFlowSettingsDialog extends ResizableDialog implements SelectionListener<Grid<FlowParameter>, FlowParameter> {

    ApplicationContext context;

    Flow flow;
    
    List<FlowParameter> flowParameterList = new ArrayList<FlowParameter>();

    Grid<FlowParameter> grid;

    Button insertButton;

    Button removeButton;
    
    public EditFlowSettingsDialog(ApplicationContext context, Flow flow, boolean readOnly) {
        super("Flow Settings");
        this.context = context;
        this.flow = flow;
        
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            setWidth((details.getWindowInnerWidth() * .75) + "px");
            setHeight((details.getWindowInnerHeight() * .75) + "px");
        });

        Button closeButton = new Button("Close");
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addClickListener(new CloseClickListener());
        
        add(new H3("General Settings"));
        
        FormLayout formLayout = new FormLayout();
        formLayout.setWidthFull();
        formLayout.setResponsiveSteps(new ResponsiveStep("0", 1));
        add(formLayout);
        
        TextArea description = new TextArea();
        description.setWidthFull();
        description.getStyle().set("max-height", "124px");
        description.setValueChangeMode(ValueChangeMode.LAZY);
        description.setValueChangeTimeout(200);
        description.addValueChangeListener(event -> {
            flow.setNotes(event.getValue());
            context.getConfigurationService().save(flow);
        });
        if (flow.getNotes() != null) {
            description.setValue(flow.getNotes());
        }
        formLayout.addFormItem(description, "Notes");

        add(new H3("Parameters"));
        
        if (!readOnly) {
            ButtonBar buttonBar = new ButtonBar();
            buttonBar.addButton("Add", VaadinIcon.PLUS, new AddClickListener());
            insertButton = buttonBar.addButton("Insert", VaadinIcon.CHEVRON_RIGHT, new InsertClickListener());
            insertButton.setEnabled(false);
            removeButton = buttonBar.addButton("Remove", VaadinIcon.TRASH, new RemoveClickListener());
            removeButton.setEnabled(false);
            add(buttonBar);
        }
        
        grid = new Grid<FlowParameter>();
        grid.setSizeFull();
        grid.addColumn(FlowParameter::getPosition).setHeader("#").setSortable(false);
        if (!readOnly) {
            grid.setRowsDraggable(true);
            grid.addDropListener(event -> saveAllPositions());
            grid.addSelectionListener(this);
            
            Editor<FlowParameter> editor = grid.getEditor();
            Binder<FlowParameter> binder = new Binder<FlowParameter>();
            editor.setBinder(binder);
            TextField nameField = createEditorField();
            binder.forField(nameField).bind(FlowParameter::getName, FlowParameter::setName);
            grid.addColumn(FlowParameter::getName).setEditorComponent(nameField).setHeader("Name").setFlexGrow(3)
                    .setSortable(false);
            TextField defaultValueField = createEditorField();
            binder.forField(defaultValueField).bind(FlowParameter::getDefaultValue, FlowParameter::setDefaultValue);
            grid.addColumn(FlowParameter::getDefaultValue).setEditorComponent(defaultValueField)
                    .setHeader("Default Value").setFlexGrow(6).setSortable(false);
            editor.addSaveListener(event -> {
                context.getConfigurationService().save(event.getItem());
            });
            grid.addItemDoubleClickListener(event -> editor.editItem(event.getItem()));
        } else {
            grid.addColumn(FlowParameter::getName).setHeader("Name").setFlexGrow(3).setSortable(false);
            grid.addColumn(FlowParameter::getDefaultValue).setHeader("Default Value").setFlexGrow(6).setSortable(false);
        }
        add(grid);

        buildButtonFooter(closeButton);

        List<FlowParameter> params = flow.getFlowParameters();
        Collections.sort(params, new Comparator<FlowParameter>() {
            public int compare(FlowParameter o1, FlowParameter o2) {
                return Integer.valueOf(o1.getPosition()).compareTo(Integer.valueOf(o2.getPosition()));
            }
        });

        grid.setItems(params);
    }

    public void selectionChange(SelectionEvent<Grid<FlowParameter>, FlowParameter> event) {
        removeButton.setEnabled(!grid.getSelectedItems().isEmpty());
        insertButton.setEnabled(!grid.getSelectedItems().isEmpty());
    }

    protected void addItem(int index) {
        FlowParameter parameter = new FlowParameter();
        parameter.setFlowId(flow.getId());
        parameter.setName("Parameter " + (index + 1));
        parameter.setPosition((index + 1));
        context.getConfigurationService().save(parameter);
        flow.getFlowParameters().add(parameter);
        flowParameterList.add(index, parameter);
        grid.setItems(flowParameterList);
        grid.select(parameter);
    }

    protected void saveAllPositions() {
        int count = 1;
        for (FlowParameter parameter : flowParameterList) {
            parameter.setPosition(count++);
            context.getConfigurationService().save(parameter);
        }
    }

    class AddClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            addItem(flow.getFlowParameters().size());
        }
    }

    class InsertClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            FlowParameter parameter = grid.getSelectionModel().getFirstSelectedItem().orElse(null);
            if (parameter != null) {
                addItem(flowParameterList.indexOf(parameter));
                saveAllPositions();
            }
        }
    }

    class RemoveClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            FlowParameter parameter = grid.getSelectionModel().getFirstSelectedItem().orElse(null);
            if (parameter != null) {
                flow.getFlowParameters().remove(parameter);
                context.getConfigurationService().delete((AbstractObject) parameter);
                int index = flowParameterList.indexOf(parameter);
                flowParameterList.remove(parameter);
                grid.setItems(flowParameterList);
                if (index < flowParameterList.size()) {
                    grid.select(flowParameterList.get(index));
                }
                saveAllPositions();
            }
        }
    }

    class CloseClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            EditFlowSettingsDialog.this.close();
        }
    }
    
    protected TextField createEditorField() {
        final TextField textField = new TextField();
        textField.setValueChangeMode(ValueChangeMode.LAZY);
        textField.setValueChangeTimeout(200);
        textField.setWidthFull();
        return textField;
    }

}
