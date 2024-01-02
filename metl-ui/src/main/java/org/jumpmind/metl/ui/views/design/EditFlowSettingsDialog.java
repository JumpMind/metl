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
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.event.selection.SelectionEvent;
import com.vaadin.event.selection.SelectionListener;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Grid;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.GridRowDragger;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
class EditFlowSettingsDialog extends ResizableWindow implements SelectionListener<FlowParameter> {

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

        Button closeButton = new Button("Close");
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        closeButton.addClickListener(new CloseClickListener());
        
        addHeader("General Settings");
        
        FormLayout formLayout = new FormLayout();
        formLayout.setMargin(true);
        formLayout.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
        formLayout.setWidth(100, Unit.PERCENTAGE);
        addComponent(formLayout);
        
        TextArea description = new TextArea("Notes");
        description.setValueChangeMode(ValueChangeMode.LAZY);
        description.setValueChangeTimeout(200);
        description.addValueChangeListener(event -> {
            flow.setNotes(event.getValue());
            context.getConfigurationService().save(flow);
        });
        if (flow.getNotes() != null) {
            description.setValue(flow.getNotes());
        }
        formLayout.addComponent(description);

        addHeader("Parameters");
        
        if (!readOnly) {
            ButtonBar buttonBar = new ButtonBar();
            buttonBar.addButton("Add", VaadinIcons.PLUS, new AddClickListener());
            insertButton = buttonBar.addButton("Insert", VaadinIcons.CHEVRON_RIGHT, new InsertClickListener());
            insertButton.setEnabled(false);
            removeButton = buttonBar.addButton("Remove", VaadinIcons.TRASH, new RemoveClickListener());
            removeButton.setEnabled(false);
            addComponent(buttonBar);
        }
        
        VerticalLayout gridWrapperLayout = new VerticalLayout();
        gridWrapperLayout.setMargin(true);
        gridWrapperLayout.setSizeFull();
        
        grid = new Grid<FlowParameter>();
        grid.setSizeFull();
        grid.addColumn(FlowParameter::getPosition).setCaption("#").setSortable(false);
        if (!readOnly) {
            new GridRowDragger<FlowParameter>(grid).getGridDropTarget().addGridDropListener(event -> saveAllPositions());
            grid.addSelectionListener(this);
            
            grid.addColumn(FlowParameter::getName).setEditorComponent(createEditorField(), FlowParameter::setName)
                    .setCaption("Name").setExpandRatio(3).setSortable(false);
            grid.addColumn(FlowParameter::getDefaultValue).setEditorComponent(createEditorField(), FlowParameter::setDefaultValue)
                    .setCaption("Default Value").setExpandRatio(6).setSortable(false);
            grid.getEditor().setEnabled(true).addSaveListener(event -> {
                context.getConfigurationService().save(event.getBean());
            });
        } else {
            grid.addColumn(FlowParameter::getName).setCaption("Name").setExpandRatio(3).setSortable(false);
            grid.addColumn(FlowParameter::getDefaultValue).setCaption("Default Value").setExpandRatio(6).setSortable(false);
        }
        gridWrapperLayout.addComponent(grid);
        
        addComponent(gridWrapperLayout, 1);

        addComponent(buildButtonFooter(closeButton));

        List<FlowParameter> params = flow.getFlowParameters();
        Collections.sort(params, new Comparator<FlowParameter>() {
            public int compare(FlowParameter o1, FlowParameter o2) {
                return new Integer(o1.getPosition()).compareTo(new Integer(o2.getPosition()));
            }
        });

        grid.setItems(params);
    }
    
    protected void addHeader(String caption) {
        HorizontalLayout componentHeaderWrapper = new HorizontalLayout();
        componentHeaderWrapper.setMargin(new MarginInfo(false, false, false, true));
        Label componentHeader = new Label(caption);
        componentHeader.addStyleName(ValoTheme.LABEL_H3);
        componentHeader.addStyleName(ValoTheme.LABEL_COLORED);
        componentHeaderWrapper.addComponent(componentHeader);
        addComponent(componentHeaderWrapper);
    }

    public void selectionChange(SelectionEvent<FlowParameter> event) {
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

    class AddClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            addItem(flow.getFlowParameters().size());
        }
    }

    class InsertClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            FlowParameter parameter = ((SingleSelectionModel<FlowParameter>) grid.getSelectionModel()).getSelectedItem().orElse(null);
            if (parameter != null) {
                addItem(flowParameterList.indexOf(parameter));
                saveAllPositions();
            }
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            FlowParameter parameter = ((SingleSelectionModel<FlowParameter>) grid.getSelectionModel()).getSelectedItem().orElse(null);
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

    class CloseClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            EditFlowSettingsDialog.this.close();
        }
    }
    
    protected TextField createEditorField() {
        final TextField textField = new TextField();
        textField.setValueChangeMode(ValueChangeMode.LAZY);
        textField.setValueChangeTimeout(200);
        textField.setWidth(100, Unit.PERCENTAGE);
        return textField;
    }

}
