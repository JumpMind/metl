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

import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.AgentFlowDeployParm;
import org.jumpmind.metl.core.model.DeploymentStatus;
import org.jumpmind.metl.core.model.StartType;
import org.jumpmind.metl.core.runtime.LogLevel;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.Field;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;

@SuppressWarnings("serial")
public class EditAgentDeploymentPanel extends VerticalSplitPanel implements IUiPanel {

    ApplicationContext context;
    
    AgentDeploy agentDeployment;

    Table table;

    ComboBox startTypeCombo;

    HorizontalLayout cronLayout;
    
    TextField startExpressionTextField;
    
    IAgentDeploymentChangeListener listener;
    
    TabbedPanel tabbedPanel;
        
    public EditAgentDeploymentPanel(ApplicationContext context, AgentDeploy agentDeployment, IAgentDeploymentChangeListener listener, 
            TabbedPanel tabbedPanel) {
        this.context = context;
        this.agentDeployment = agentDeployment;
        this.listener = listener;
        this.tabbedPanel = tabbedPanel;

        VerticalLayout vlay = new VerticalLayout();
        FormLayout form = new FormLayout();
        form.setSpacing(true);
        form.setMargin(true);
        form.addComponent(getNameComponent());
        form.addComponent(getLogLevelComponent());
        form.addComponent(getStartTypeComponent());

        vlay.addComponent(form);
        cronLayout = new HorizontalLayout();
        cronLayout.setSpacing(true);
        cronLayout.setMargin(true);
        cronLayout.addComponent(getScheduleComponent("Second"));
        cronLayout.addComponent(getScheduleComponent("Minute"));
        cronLayout.addComponent(getScheduleComponent("Hour"));
        cronLayout.addComponent(getScheduleComponent("Day"));
        cronLayout.addComponent(getScheduleComponent("Month"));
        cronLayout.addComponent(getScheduleComponent("Day of Week"));
        cronLayout.addComponent(getScheduleComponent("Year"));
        vlay.addComponent(cronLayout);

        FormLayout cronForm = new FormLayout();
        cronForm.setSpacing(true);
        cronForm.setMargin(true);
        cronForm.addComponent(getCronComponent());
        vlay.addComponent(cronForm);

        table = new Table();
        table.setSizeFull();
        BeanItemContainer<AgentFlowDeployParm> container = new BeanItemContainer<AgentFlowDeployParm>(AgentFlowDeployParm.class);
        table.setContainerDataSource(container);
        table.setEditable(true);
        table.setSelectable(true);
        table.setTableFieldFactory(new EditFieldFactory());
        table.setVisibleColumns("name", "value");
        table.setColumnHeaders("Parameter Name", "Value");

        container.addAll(agentDeployment.getAgentDeploymentParms());

        setSplitPosition(60f);
        setFirstComponent(vlay);
        setSecondComponent(table);
        updateScheduleEnable();
        updateScheduleFields();
    }
    
    @Override
    public boolean closing() {
        AgentDeploy deployment = context.getOperationsService().findAgentDeployment(agentDeployment.getId());
        if (deployment.getStatus().equals(DeploymentStatus.ENABLED.name())) {
            deployment.setStatus(DeploymentStatus.REQUEST_REENABLE.name());
            context.getConfigurationService().save(deployment);
        }
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    protected void saveAgentDeployment(AgentDeploy agentDeployment) {
        context.getConfigurationService().save(agentDeployment);
        listener.changed(agentDeployment);
    }

    protected TextField getNameComponent() {
        ImmediateUpdateTextField textField = new ImmediateUpdateTextField("Name") {
            protected void save(String text) {
                agentDeployment.setName(text);
                saveAgentDeployment(agentDeployment);
            }            
        };
        textField.setWidth(300, Unit.PIXELS);
        textField.setValue(agentDeployment.getName());
        return textField;
    }

    protected ComboBox getLogLevelComponent() {
        final ComboBox combo = new ComboBox("Log Level");
        combo.setNullSelectionAllowed(false);
        combo.setWidth(200, Unit.PIXELS);
        LogLevel[] levels = LogLevel.values();
        for (LogLevel logLevel : levels) {
            combo.addItem(logLevel.name());
        }
        combo.setValue(agentDeployment.getLogLevel());
        combo.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                agentDeployment.setLogLevel((String) combo.getValue());
                saveAgentDeployment(agentDeployment);
            }
        });
        return combo;
    }

    protected ComboBox getStartTypeComponent() {
        startTypeCombo = new ComboBox("Start Type");
        startTypeCombo.setWidth(200, Unit.PIXELS);
        startTypeCombo.setNullSelectionAllowed(false);
        StartType[] values = StartType.values();
        for (StartType value : values) {
            startTypeCombo.addItem(value.name());
        }
        startTypeCombo.setValue(agentDeployment.getStartType());
        startTypeCombo.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                agentDeployment.setStartType((String) startTypeCombo.getValue());
                updateScheduleEnable();
                for (int i = 0; i < 7; i++) {
                    ListSelect listSelect = ((ListSelect) cronLayout.getComponent(i));
                    for (Object itemId : listSelect.getItemIds()) {
                        listSelect.unselect(itemId);
                    }
                    listSelect.select(listSelect.getItemIds().iterator().next());
                }
                String startExpression = null;
                if (agentDeployment.getStartType().equals(StartType.SCHEDULED_CRON.name())) {
                    startExpression = "0 0 0 * * ?";
                }
                startExpressionTextField.setValue(startExpression);
                agentDeployment.setStartExpression(startExpression);
                updateScheduleFields();
                saveAgentDeployment(agentDeployment);
            }
        });
        return startTypeCombo;
    }

    protected ListSelect getScheduleComponent(String caption) {
        ListSelect listSelect = new ListSelect(caption);
        listSelect.setWidth(90, Unit.PIXELS);
        listSelect.setMultiSelect(true);
        listSelect.setRows(10);
        listSelect.setImmediate(true);
        listSelect.addItem("*");
        listSelect.setItemCaption("*", "<All>");
        if (caption.equals("Second") || caption.equals("Minute")) {
            for (int i = 0; i <= 59; i++) {
                listSelect.addItem(String.valueOf(i));
            }
        } else if (caption.equals("Hour")) {
            for (int i = 0; i <= 23; i++) {
                listSelect.addItem(String.valueOf(i));
            }            
        } else if (caption.equals("Day")) {
            for (int i = 1; i <= 31; i++) {
                listSelect.addItem(String.valueOf(i));
            }            
        } else if (caption.equals("Month")) {
            listSelect.addItems("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
        } else if (caption.equals("Day of Week")) {
            listSelect.addItems("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");
        } else if (caption.equals("Year")) {
            for (int i = Calendar.getInstance().get(Calendar.YEAR); i <= 2099; i++) {
                listSelect.addItem(String.valueOf(i));
            }
        }
        listSelect.select(listSelect.getItemIds().iterator().next());
        listSelect.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                updateScheduleExpression();
            }            
        });
        return listSelect;
    }

    protected TextField getCronComponent() {
        startExpressionTextField = new ImmediateUpdateTextField("Start Expression") {
            protected void save(String text) {
                agentDeployment.setStartExpression(text);
                updateScheduleFields();
                saveAgentDeployment(agentDeployment);
            }
        };
        startExpressionTextField.setWidth(275, Unit.PIXELS);
        startExpressionTextField.setValue(agentDeployment.getStartExpression());
        return startExpressionTextField;
    }

    protected void updateScheduleEnable() {
        boolean isCron = startTypeCombo.getValue().equals(StartType.SCHEDULED_CRON.name());
        Iterator<Component> iter = cronLayout.iterator();
        while (iter.hasNext()) {
            iter.next().setEnabled(isCron);
        }
        startExpressionTextField.setEnabled(false);
    }

    protected void updateScheduleExpression() {
        StringBuilder sb = new StringBuilder();
        Set<Object> secondValues = getCronValue(0);
        Set<Object> minuteValues = getCronValue(1);
        Set<Object> hourValues = getCronValue(2);
        Set<Object> dayValues = getCronValue(3);
        Set<Object> monthValues = getCronValue(4);
        Set<Object> dayOfWeekValues = getCronValue(5);
        Set<Object> yearValues = getCronValue(6);
        
        if (dayOfWeekValues.contains("*")) {
            dayOfWeekValues = new HashSet<Object>();
            dayOfWeekValues.add("?");
        } else if (dayValues.contains("*")) {
            dayValues = new HashSet<Object>();
            dayValues.add("?");
        }
        if (yearValues.contains("*")) {
            yearValues = new HashSet<Object>();
        }
        
        append(sb, secondValues).append(" ");
        append(sb, minuteValues).append(" ");
        append(sb, hourValues).append(" ");
        append(sb, dayValues).append(" ");
        append(sb, monthValues).append(" ");
        append(sb, dayOfWeekValues).append(" ");
        append(sb, yearValues);
        startExpressionTextField.setValue(sb.toString());
        agentDeployment.setStartExpression(sb.toString());
        saveAgentDeployment(agentDeployment);
    }

    protected void updateScheduleFields() {
        if (agentDeployment.getStartExpression() != null) {
            String[] fields = agentDeployment.getStartExpression().split(" ");
            for (int i = 0; i < fields.length; i++) {
                if (fields[i].equals("?")) {
                    fields[i] = "*";
                }
                String[] values = fields[i].split(",");
                ListSelect listSelect = ((ListSelect) cronLayout.getComponent(i));
                for (Object itemId : listSelect.getItemIds()) {
                    listSelect.unselect(itemId);
                }
                for (String value : values) {
                    listSelect.select(value);
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    protected Set<Object> getCronValue(int index) {
        Set<Object> values = (Set<Object>) ((ListSelect) cronLayout.getComponent(index)).getValue();
        if (values.contains("*") && values.size() > 1) {
            values = new HashSet<Object>();
            values.add("*");
        }
        return values;
    }
    
    protected StringBuilder append(StringBuilder sb, Set<Object> values) {
        Iterator<Object> valueIter = values.iterator();
        while (valueIter.hasNext()) {
            sb.append(valueIter.next().toString());
            if (valueIter.hasNext()) {
                sb.append(",");
            }
        }
        return sb;
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId,
                final Object propertyId, com.vaadin.ui.Component uiContext) {
            if (propertyId.equals("value")) {
                final AgentFlowDeployParm parameter = (AgentFlowDeployParm) itemId;
                final TextField textField = new ImmediateUpdateTextField(null) {
                    protected void save(String text) {
                        parameter.setValue(text);
                        context.getConfigurationService().save(parameter);
                    }
                };
                textField.setWidth(100, Unit.PERCENTAGE);
                return textField;
            }
            return null;
        }
    }

}
