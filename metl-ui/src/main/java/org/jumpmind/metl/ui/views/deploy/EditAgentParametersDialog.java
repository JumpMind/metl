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
package org.jumpmind.metl.ui.views.deploy;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentParameter;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.FieldEvents.FocusEvent;
import com.vaadin.event.FieldEvents.FocusListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
class EditAgentParametersDialog extends ResizableWindow implements ValueChangeListener {

    ApplicationContext context;

    Agent agent;

    Table table;

    Button removeButton;

    public EditAgentParametersDialog(final ApplicationContext context, final Agent agent) {
        super("Agent Parameters");
        this.context = context;
        this.agent = agent;

        Button closeButton = new Button("Close");
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        closeButton.addClickListener(new CloseClickListener());

        ButtonBar buttonBar = new ButtonBar();
        buttonBar.addButton("Add", FontAwesome.PLUS, new AddClickListener());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O, new RemoveClickListener());
        removeButton.setEnabled(false);
        addComponent(buttonBar);

        table = new Table();
        table.setSizeFull();
        BeanItemContainer<AgentParameter> container = new BeanItemContainer<AgentParameter>(AgentParameter.class);
        table.setContainerDataSource(container);
        table.setEditable(true);
        table.setSelectable(true);
        table.setTableFieldFactory(new EditFieldFactory());
        table.setVisibleColumns("name", "value");
        table.setColumnHeaders("Name", "Value");
        table.addValueChangeListener(this);
        addComponent(table, 1);

        addComponent(buildButtonFooter(closeButton));

        if (agent.getAgentParameters() != null) {
            for (AgentParameter parameter : agent.getAgentParameters()) {
                table.addItem(parameter);
            }
        }
    }

    public void valueChange(ValueChangeEvent event) {
        removeButton.setEnabled(table.getValue() != null);
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId, final Object propertyId, Component uiContext) {
            final AgentParameter parameter = (AgentParameter) itemId;
            final TextField textField = new ImmediateUpdateTextField(null) {
                protected void save(String text) {
                    parameter.setValue(text);
                    context.getConfigurationService().save(parameter);
                }
            };
            textField.setWidth(100, Unit.PERCENTAGE);
            textField.addFocusListener(new FocusListener() {
                public void focus(FocusEvent event) {
                    table.select(itemId);
                }
            });
            return textField;
        }
    }

    class CloseClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            EditAgentParametersDialog.this.close();
        }
    }

    class AddClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            AgentParameter parameter = new AgentParameter();
            parameter.setAgentId(agent.getId());
            parameter.setName("Setting " + (agent.getAgentParameters().size() + 1));
            context.getConfigurationService().save(parameter);
            agent.getAgentParameters().add(parameter);
            table.addItem(parameter);
            table.select(parameter);
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Setting parameter = (Setting) table.getValue();
            if (parameter != null) {
                agent.getAgentParameters().remove(parameter);
                context.getConfigurationService().delete(parameter);
                table.removeItem(parameter);
            }
        }
    }
}
