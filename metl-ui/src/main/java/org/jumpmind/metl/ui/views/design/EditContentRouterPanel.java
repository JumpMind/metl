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
import java.util.Collection;
import java.util.List;

import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.ContentRouter;
import org.jumpmind.metl.core.runtime.component.ContentRouter.Route;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;

@SuppressWarnings("serial")
public class EditContentRouterPanel extends AbstractFlowStepAwareComponentEditPanel {

    Table table = new Table();

    Button addButton;

    Button removeButton;

    BeanItemContainer<Route> container = new BeanItemContainer<Route>(Route.class);

    protected void buildUI() {
        if (!readOnly) {
            ButtonBar buttonBar = new ButtonBar();
            addComponent(buttonBar);

            addButton = buttonBar.addButton("Add", FontAwesome.PLUS);
            addButton.addClickListener((event) -> table.addItem(new Route()));

            removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
            removeButton.addClickListener((event) -> {
                if (table.getValue() != null) {
                    table.removeItem(table.getValue());
                    save();
                }
            });
        }

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSizeFull(); 
        table.setVisibleColumns(new Object[] { "matchExpression", "targetStepId" });
        table.setColumnHeaders(new String[] { "Expression", "Target Step" });
        table.setTableFieldFactory(new EditFieldFactory());        
        table.addItemClickListener((event) -> {
            if (table.getValue() != null) {
                table.setValue(null);
            }
        });
        table.setEditable(true);
        if (!readOnly) {
            table.addValueChangeListener((event) -> removeButton.setEnabled(table.getValue() != null));
        }

        addComponent(table);
        setExpandRatio(table, 1.0f);


    }    
    
    @Override
    public void selected() {
        table.removeAllItems();
        String json = flowStep.getComponent().get(ContentRouter.SETTING_CONFIG);
        if (isNotBlank(json)) {
            try {
                List<Route> routes = new ObjectMapper().readValue(json, new TypeReference<List<Route>>() {
                });
                for (Route route : routes) {
                    table.addItem(route);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected void save() {
        @SuppressWarnings("unchecked")
        List<Route> routes = new ArrayList<Route>((Collection<Route>) table.getItemIds());
        try {
            Setting setting = flowStep.getComponent().findSetting(ContentRouter.SETTING_CONFIG);
            setting.setValue(new ObjectMapper().writeValueAsString(routes));
            context.getConfigurationService().save(setting);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId,
                com.vaadin.ui.Component uiContext) {
            final Route route = (Route) itemId;
            Field<?> field = null;
            if (propertyId.equals("matchExpression")) {
                final TextField textField = new ImmediateUpdateTextField(null) {
                    @Override
                    protected void save(String text) {
                        route.setMatchExpression(text);
                        EditContentRouterPanel.this.save();
                    }
                };
                textField.setWidth(100, Unit.PERCENTAGE);
                textField.setValue(route.getMatchExpression());
                field = textField;
            } else if (propertyId.equals("targetStepId")) {
                final ComboBox combo = new ComboBox();
                combo.setWidth(100, Unit.PERCENTAGE);
                flow = context.getConfigurationService().findFlow(flow.getId());
                List<FlowStepLink> stepLinks = flow.findFlowStepLinksWithSource(flowStep.getId());
                for (FlowStepLink flowStepLink : stepLinks) {
                    FlowStep comboStep = flow.findFlowStepWithId(flowStepLink.getTargetStepId());
                    combo.addItem(comboStep.getId());
                    combo.setItemCaption(comboStep.getId(), comboStep.getName());

                    if (flowStepLink.getTargetStepId().equals(route.getTargetStepId()) || combo.getValue() == null) {
                        combo.setValue(comboStep.getId());
                    }
                }

                combo.setImmediate(true);
                combo.setNewItemsAllowed(false);
                combo.setNullSelectionAllowed(false);
                combo.addValueChangeListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        String stepId = (String) event.getProperty().getValue();
                        if (stepId != null) {
                            route.setTargetStepId(stepId);
                            EditContentRouterPanel.this.save();
                        }
                    }
                });
                field = combo;
            }
            if (field != null) {
                field.setReadOnly(readOnly);
            }
            return field;
        }
    }

}
