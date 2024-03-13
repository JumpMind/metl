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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.List;

import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.ContentRouter;
import org.jumpmind.metl.core.runtime.component.ContentRouter.Route;
import org.jumpmind.metl.ui.common.ButtonBar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;

@SuppressWarnings("serial")
public class EditContentRouterPanel extends AbstractFlowStepAwareComponentEditPanel {

    List<Route> routeList = new ArrayList<Route>();

    Grid<Route> grid = new Grid<Route>();

    Button addButton;

    Button removeButton;

    protected void buildUI() {
        setPadding(false);
        setSpacing(false);
        if (!readOnly) {
            ButtonBar buttonBar = new ButtonBar();
            add(buttonBar);

            Editor<Route> editor = grid.getEditor();
            addButton = buttonBar.addButton("Add", VaadinIcon.PLUS);
            addButton.addClickListener((event) -> {
                Route newRoute = new Route();
                routeList.add(newRoute);
                grid.setItems(routeList);
                editor.editItem(newRoute);
            });

            removeButton = buttonBar.addButton("Remove", VaadinIcon.TRASH);
            removeButton.addClickListener((event) -> {
                Route selected = grid.getSelectionModel().getFirstSelectedItem().orElse(null);
                if (selected != null) {
                    routeList.remove(selected);
                    save();
                }
            });
            
            final TextField textField = new TextField();
            textField.setValueChangeMode(ValueChangeMode.LAZY);
            textField.setValueChangeTimeout(200);
            textField.setWidthFull();
            
            Binder<Route> binder = new Binder<Route>();
            editor.setBinder(binder);
            binder.forField(textField).bind(Route::getMatchExpression, Route::setMatchExpression);
            grid.addColumn(Route::getMatchExpression).setEditorComponent(textField).setHeader("Expression")
                    .setSortable(false);
            
            final ComboBox<FlowStep> combo = new ComboBox<FlowStep>();
            combo.setWidthFull();
            flow = context.getConfigurationService().findFlow(flow.getId());
            List<FlowStep> comboStepList = new ArrayList<FlowStep>();
            List<FlowStepLink> stepLinks = flow.findFlowStepLinksWithSource(flowStep.getId());
            for (FlowStepLink flowStepLink : stepLinks) {
                FlowStep comboStep = flow.findFlowStepWithId(flowStepLink.getTargetStepId());
                comboStepList.add(comboStep);
            }
            combo.setItems(comboStepList);
            if (!comboStepList.isEmpty()) {
                combo.setValue(comboStepList.iterator().next());
            }
            combo.setItemLabelGenerator(item -> item.getName());
            combo.addValueChangeListener(event -> {
                if (event.getValue() == null) {
                    combo.setValue(event.getOldValue());
                }
            });
            binder.forField(combo).bind(route -> flow.findFlowStepWithId(route.getTargetStepId()),
                    (route, step) -> route.setTargetStepId(step.getId()));
            grid.addColumn(Route::getTargetStepId).setEditorComponent(combo).setHeader("Target Step").setSortable(false);
            
            editor.addSaveListener(event -> save());
            grid.addItemDoubleClickListener(event -> editor.editItem(event.getItem()));
            
            grid.addSelectionListener((event) -> removeButton.setEnabled(!event.getAllSelectedItems().isEmpty()));
        } else {
            grid.addColumn(Route::getMatchExpression).setHeader("Expression").setSortable(false);
            grid.addColumn(Route::getTargetStepId).setHeader("Target Step").setSortable(false);
        }

        grid.setSizeFull(); 

        add(grid);
        expand(grid);


    }    
    
    @Override
    public void selected() {
        routeList.clear();
        String json = flowStep.getComponent().get(ContentRouter.SETTING_CONFIG);
        if (isNotBlank(json)) {
            try {
                List<Route> routes = new ObjectMapper().readValue(json, new TypeReference<List<Route>>() {
                });
                for (Route route : routes) {
                    routeList.add(route);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        grid.setItems(routeList);
    }

    protected void save() {
        try {
            Setting setting = flowStep.getComponent().findSetting(ContentRouter.SETTING_CONFIG);
            setting.setValue(new ObjectMapper().writeValueAsString(routeList));
            context.getConfigurationService().save(setting);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
