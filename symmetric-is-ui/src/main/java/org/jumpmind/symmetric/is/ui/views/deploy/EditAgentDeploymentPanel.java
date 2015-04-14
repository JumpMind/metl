package org.jumpmind.symmetric.is.ui.views.deploy;

import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.FlowParameter;
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
    
    HorizontalLayout cronLayout;
    
    TextField cronTextField;
        
    public EditAgentDeploymentPanel(ApplicationContext context, AgentDeployment agentDeployment) {
        this.context = context;
        this.agentDeployment = agentDeployment;

        FormLayout cronForm = new FormLayout();
        cronForm.setSpacing(true);
        cronForm.setMargin(true);
        cronTextField = new TextField("Cron");
        cronTextField.setWidth(250, Unit.PIXELS);
        cronForm.addComponent(cronTextField);

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

        VerticalLayout vlay = new VerticalLayout();
        FormLayout form = new FormLayout();
        form.setSpacing(true);
        form.setMargin(true);
        form.addComponent(getNameComponent());
        form.addComponent(getLogLevelComponent());
        form.addComponent(getStartTypeComponent());
        vlay.addComponent(form);        
        vlay.addComponent(cronLayout);
        vlay.addComponent(cronForm);

        table = new Table();
        table.setSizeFull();
        BeanItemContainer<FlowParameter> container = new BeanItemContainer<FlowParameter>(FlowParameter.class);
        table.setContainerDataSource(container);
        table.setEditable(true);
        table.setSelectable(true);
        table.setTableFieldFactory(new EditFieldFactory());
        table.setVisibleColumns("name", "defaultValue");
        table.setColumnHeaders("Parameter Name", "Value");
        table.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
            }
        });

        List<FlowParameter> params = agentDeployment.getFlow().getFlowParameters();
        Collections.sort(params, new Comparator<FlowParameter>() {
            public int compare(FlowParameter o1, FlowParameter o2) {
                return new Integer(o1.getPosition()).compareTo(new Integer(o2.getPosition()));
            }
        });

        for (FlowParameter flowParameter : params) {
            table.addItem(flowParameter);
        }

        setSplitPosition(55f);
        setFirstComponent(vlay);
        setSecondComponent(table);
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
        TextField textField = new TextField("Name");
        textField.setWidth(300, Unit.PIXELS);
        textField.setValue(agentDeployment.getName());
        return textField;
    }

    protected ComboBox getLogLevelComponent() {
        ComboBox combo = new ComboBox("Log Level");
        combo.setNullSelectionAllowed(false);
        combo.setWidth(200, Unit.PIXELS);
        LogLevel[] levels = LogLevel.values();
        for (LogLevel logLevel : levels) {
            combo.addItem(logLevel.name());
        }
        combo.setValue(agentDeployment.getLogLevel());
        return combo;
    }

    protected ComboBox getStartTypeComponent() {
        final ComboBox combo = new ComboBox("Start Type");
        combo.setWidth(200, Unit.PIXELS);
        combo.setNullSelectionAllowed(false);
        StartType[] values = StartType.values();
        for (StartType value : values) {
            combo.addItem(value.name());
        }
        combo.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                boolean isCron = combo.getValue().equals(StartType.SCHEDULED_CRON.name());
                cronTextField.setEnabled(isCron);
                Iterator<Component> iter = cronLayout.iterator();
                while (iter.hasNext()) {
                    iter.next().setEnabled(isCron);
                }
                cronTextField.setEnabled(isCron);
            }
        });
        combo.setValue(agentDeployment.getStartType());
        return combo;
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
    
    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId,
                final Object propertyId, com.vaadin.ui.Component uiContext) {
            if (propertyId.equals("defaultValue")) {
                final FlowParameter parameter = (FlowParameter) itemId;
                final TextField textField = new ImmediateUpdateTextField(null) {
                    protected void save() {
                        //context.getConfigurationService().save(parameter);
                    }
                };
                textField.setWidth(100, Unit.PERCENTAGE);
                return textField;
            }
            return null;
        }
    }

}
