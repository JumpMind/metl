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
import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.ValueChangeMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Grid;
import com.vaadin.ui.TextField;
import com.vaadin.ui.components.grid.SingleSelectionModel;

@SuppressWarnings("serial")
public class EditContentRouterPanel extends AbstractFlowStepAwareComponentEditPanel {

    List<Route> routeList = new ArrayList<Route>();

    Grid<Route> grid = new Grid<Route>();

    Button addButton;

    Button removeButton;

    protected void buildUI() {
        if (!readOnly) {
            ButtonBar buttonBar = new ButtonBar();
            addComponent(buttonBar);

            addButton = buttonBar.addButton("Add", VaadinIcons.PLUS);
            addButton.addClickListener((event) -> {
                routeList.add(new Route());
                grid.setItems(routeList);
            });

            removeButton = buttonBar.addButton("Remove", VaadinIcons.TRASH);
            removeButton.addClickListener((event) -> {
                Route selected = ((SingleSelectionModel<Route>) grid.getSelectionModel()).getSelectedItem().orElse(null);
                if (selected != null) {
                    routeList.remove(selected);
                    save();
                }
            });
            
            final TextField textField = new TextField();
            textField.setValueChangeMode(ValueChangeMode.LAZY);
            textField.setValueChangeTimeout(200);
            textField.setWidth(100, Unit.PERCENTAGE);
            grid.addColumn(Route::getMatchExpression).setEditorComponent(textField, Route::setMatchExpression)
                    .setCaption("Expression").setSortable(false);
            
            final ComboBox<FlowStep> combo = new ComboBox<FlowStep>();
            combo.setWidth(100, Unit.PERCENTAGE);
            flow = context.getConfigurationService().findFlow(flow.getId());
            List<FlowStep> comboStepList = new ArrayList<FlowStep>();
            List<FlowStepLink> stepLinks = flow.findFlowStepLinksWithSource(flowStep.getId());
            for (FlowStepLink flowStepLink : stepLinks) {
                FlowStep comboStep = flow.findFlowStepWithId(flowStepLink.getTargetStepId());
                comboStepList.add(comboStep);
            }
            combo.setItems(comboStepList);
            combo.setItemCaptionGenerator(item -> item.getName());
            combo.setEmptySelectionAllowed(false);
            grid.addColumn(Route::getTargetStepId).setCaption("Target Step").setSortable(false);
            
            grid.getEditor().setEnabled(true).addSaveListener(event -> save());
            
            grid.addSelectionListener((event) -> removeButton.setEnabled(!event.getAllSelectedItems().isEmpty()));
        } else {
            grid.addColumn(Route::getMatchExpression).setCaption("Expression").setSortable(false);
            grid.addColumn(Route::getTargetStepId).setCaption("Target Step").setSortable(false);
        }

        grid.setSizeFull(); 

        addComponent(grid);
        setExpandRatio(grid, 1.0f);


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
