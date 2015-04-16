package org.jumpmind.symmetric.is.ui.views.deploy;

import java.util.Calendar;
import java.util.Iterator;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentParameter;
import org.jumpmind.symmetric.is.core.model.StartType;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;

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
    
    AgentDeployment agentDeployment;

    Table table;

    ComboBox startTypeCombo;

    HorizontalLayout cronLayout;
    
    TextField cronTextField;
        
    public EditAgentDeploymentPanel(ApplicationContext context, AgentDeployment agentDeployment) {
        this.context = context;
        this.agentDeployment = agentDeployment;

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
        BeanItemContainer<AgentDeploymentParameter> container = new BeanItemContainer<AgentDeploymentParameter>(AgentDeploymentParameter.class);
        table.setContainerDataSource(container);
        table.setEditable(true);
        table.setSelectable(true);
        table.setTableFieldFactory(new EditFieldFactory());
        table.setVisibleColumns("name", "value");
        table.setColumnHeaders("Parameter Name", "Value");

        container.addAll(agentDeployment.getAgentDeploymentParameters());

        setSplitPosition(55f);
        setFirstComponent(vlay);
        setSecondComponent(table);
        checkScheduleEnable();
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

    protected TextField getNameComponent() {
        ImmediateUpdateTextField textField = new ImmediateUpdateTextField("Name") {
            protected void save() {
                agentDeployment.setName(getValue());
                context.getConfigurationService().save(agentDeployment);
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
                context.getConfigurationService().save(agentDeployment);
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
                context.getConfigurationService().save(agentDeployment);
                checkScheduleEnable();
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
        listSelect.addItem("<All>");
        if (caption.equals("Second") || caption.equals("Minute")) {
            for (int i = 0; i <= 59; i++) {
                listSelect.addItem(i);
            }
        } else if (caption.equals("Hour")) {
            for (int i = 0; i <= 23; i++) {
                listSelect.addItem(i);
            }            
        } else if (caption.equals("Day")) {
            for (int i = 1; i <= 31; i++) {
                listSelect.addItem(i);
            }            
        } else if (caption.equals("Month")) {
            listSelect.addItems("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC");
        } else if (caption.equals("Day of Week")) {
            listSelect.addItems("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT");
        } else if (caption.equals("Year")) {
            for (int i = Calendar.getInstance().get(Calendar.YEAR); i <= 2099; i++) {
                listSelect.addItem(i);
            }            
        }
        listSelect.select(listSelect.getItemIds().iterator().next());
        return listSelect;
    }

    protected TextField getCronComponent() {
        cronTextField = new ImmediateUpdateTextField("Start Expression") {
            protected void save() {
                agentDeployment.setStartExpression(getValue());
                context.getConfigurationService().save(agentDeployment);
            }
        };
        cronTextField.setWidth(275, Unit.PIXELS);
        cronTextField.setValue(agentDeployment.getStartExpression());
        return cronTextField;
    }

    protected void checkScheduleEnable() {
        boolean isCron = startTypeCombo.getValue().equals(StartType.SCHEDULED_CRON.name());
        cronTextField.setEnabled(isCron);
        Iterator<Component> iter = cronLayout.iterator();
        while (iter.hasNext()) {
            iter.next().setEnabled(isCron);
        }
        cronTextField.setEnabled(isCron);
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId,
                final Object propertyId, com.vaadin.ui.Component uiContext) {
            if (propertyId.equals("value")) {
                final AgentDeploymentParameter parameter = (AgentDeploymentParameter) itemId;
                final TextField textField = new ImmediateUpdateTextField(null) {
                    protected void save() {
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
