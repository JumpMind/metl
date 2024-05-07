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
import com.vaadin.flow.component.grid.dnd.GridDropLocation;
import com.vaadin.flow.component.grid.dnd.GridDropMode;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.selection.SelectionEvent;
import com.vaadin.flow.data.selection.SelectionListener;
import com.vaadin.flow.data.value.ValueChangeMode;

@SuppressWarnings("serial")
class EditFlowSettingsDialog extends ResizableDialog implements SelectionListener<Grid<FlowParameter>, FlowParameter> {

    ApplicationContext context;

    Flow flow;
    
    List<FlowParameter> flowParameterList = new ArrayList<FlowParameter>();

    Grid<FlowParameter> grid;
    
    FlowParameter draggedParameter;

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
        grid.addColumn(FlowParameter::getPosition).setHeader("#").setFlexGrow(0).setWidth("100px").setSortable(false);
        if (!readOnly) {
            grid.setRowsDraggable(true);
            grid.setDropMode(GridDropMode.BETWEEN);
            grid.addDragStartListener(
                    event -> draggedParameter = !event.getDraggedItems().isEmpty() ? event.getDraggedItems().get(0) : null);
            grid.addDropListener(event -> {
                if (draggedParameter != null) {
                    FlowParameter dropTargetParameter = event.getDropTargetItem().orElse(null);
                    if (dropTargetParameter != null && !draggedParameter.equals(dropTargetParameter)) {
                        flowParameterList.remove(draggedParameter);
                        int index = flowParameterList.indexOf(dropTargetParameter);
                        if (event.getDropLocation() == GridDropLocation.BELOW) {
                            flowParameterList.add(index + 1, draggedParameter);
                        } else {
                            flowParameterList.add(index, draggedParameter);
                        }
                        saveAllPositions();
                        grid.setItems(flowParameterList);
                    }
                }
            });
            grid.addDragEndListener(event -> draggedParameter = null);
            grid.addSelectionListener(this);
            
            grid.addComponentColumn(parameter -> {
                TextField nameField = createEditorField();
                nameField.setValue(parameter.getName() != null ? parameter.getName() : "");
                nameField.addValueChangeListener(event -> {
                    parameter.setName(event.getValue());
                    context.getConfigurationService().save(parameter);
                });
                return nameField;
            }).setHeader("Name").setFlexGrow(3).setSortable(false);
            grid.addComponentColumn(parameter -> {
                TextField defaultValueField = createEditorField();
                defaultValueField.setValue(parameter.getDefaultValue() != null ? parameter.getDefaultValue() : "");
                defaultValueField.addValueChangeListener(event -> {
                    parameter.setDefaultValue(event.getValue());
                    context.getConfigurationService().save(parameter);
                });
                return defaultValueField;
            }).setHeader("Default Value").setFlexGrow(6).setSortable(false);
        } else {
            grid.addColumn(FlowParameter::getName).setHeader("Name").setFlexGrow(3).setSortable(false);
            grid.addColumn(FlowParameter::getDefaultValue).setHeader("Default Value").setFlexGrow(6).setSortable(false);
        }
        add(grid);

        buildButtonFooter(closeButton);

        flowParameterList = flow.getFlowParameters();
        Collections.sort(flowParameterList, new Comparator<FlowParameter>() {
            public int compare(FlowParameter o1, FlowParameter o2) {
                return Integer.valueOf(o1.getPosition()).compareTo(Integer.valueOf(o2.getPosition()));
            }
        });

        grid.setItems(flowParameterList);
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
                context.getConfigurationService().delete((AbstractObject) parameter);
                int index = flowParameterList.indexOf(parameter);
                flowParameterList.remove(parameter);
                grid.setItems(flowParameterList);
                if (index > -1 && index < flowParameterList.size()) {
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
        textField.setWidthFull();
        textField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        textField.setValueChangeMode(ValueChangeMode.LAZY);
        textField.setValueChangeTimeout(200);
        return textField;
    }

}
