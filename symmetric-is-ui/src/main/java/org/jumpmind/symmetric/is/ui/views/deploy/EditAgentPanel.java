package org.jumpmind.symmetric.is.ui.views.deploy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentStartMode;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.StartType;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.LogLevel;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.util.AppUtils;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.AbstractBeanContainer.BeanIdResolver;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditAgentPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Table table;

    BeanContainer<AgentDeployment, AgentDeployment> container;

    Agent agent;

    Set<Object> lastEditItemIds = Collections.emptySet();

    Button removeButton;

    Button editButton;
    
    TabbedPanel tabbedPanel;

    FlowSelectWindow flowSelectWindow;

    public EditAgentPanel(ApplicationContext context, TabbedPanel tabbedPanel, Agent agent) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;
        this.agent = agent;
        
        HorizontalLayout editAgentLayout = new HorizontalLayout();
        editAgentLayout.setSpacing(true);
        editAgentLayout.setMargin(new MarginInfo(true, false, false, true));
        editAgentLayout.addStyleName(ValoTheme.LAYOUT_HORIZONTAL_WRAPPING);        
        addComponent(editAgentLayout);
        
        final ComboBox startModeCombo = new ComboBox("Start Mode");
        startModeCombo.setImmediate(true);
        startModeCombo.setNullSelectionAllowed(false);
        AgentStartMode[] modes = AgentStartMode.values();
        for (AgentStartMode agentStartMode : modes) {
            startModeCombo.addItem(agentStartMode.name());
        }
        startModeCombo.setValue(agent.getStartMode());
        startModeCombo.addValueChangeListener(new ValueChangeListener() {
            
            @Override
            public void valueChange(ValueChangeEvent event) {
                EditAgentPanel.this.agent.setStartMode((String)startModeCombo.getValue());
                EditAgentPanel.this.context.getConfigurationService().save((AbstractObject)EditAgentPanel.this.agent);
            }
        });
        
        editAgentLayout.addComponent(startModeCombo);
        editAgentLayout.setComponentAlignment(startModeCombo, Alignment.BOTTOM_LEFT);
        
        HorizontalLayout buttonGroup = new HorizontalLayout();
        
        final TextField hostNameField = new TextField("Hostname");
        hostNameField.setImmediate(true);
        hostNameField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        hostNameField.setTextChangeTimeout(100);
        hostNameField.setNullRepresentation("");
        hostNameField.setValue(agent.getHost());
        hostNameField.addValueChangeListener(new ValueChangeListener() {
            
            @Override
            public void valueChange(ValueChangeEvent event) {
                EditAgentPanel.this.agent.setHost((String)hostNameField.getValue());
                EditAgentPanel.this.context.getConfigurationService().save((AbstractObject)EditAgentPanel.this.agent);                
            }
        });
        buttonGroup.addComponent(hostNameField);
        buttonGroup.setComponentAlignment(hostNameField, Alignment.BOTTOM_LEFT);
        
        Button getHostNameButton = new Button("Get Host");
        getHostNameButton.addClickListener(new ClickListener() {
            
            @Override
            public void buttonClick(ClickEvent event) {
                hostNameField.setValue(AppUtils.getHostName());
            }
        });
        buttonGroup.addComponent(getHostNameButton);
        buttonGroup.setComponentAlignment(getHostNameButton, Alignment.BOTTOM_LEFT);
        
        editAgentLayout.addComponent(buttonGroup);
        editAgentLayout.setComponentAlignment(buttonGroup, Alignment.BOTTOM_LEFT);
        

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button addDeploymentButton = buttonBar.addButton("Add Deployment", Icons.DEPLOYMENT);
        addDeploymentButton.addClickListener(new AddDeploymentClickListener());

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new RemoveClickListener());

        container = new BeanContainer<AgentDeployment, AgentDeployment>(AgentDeployment.class);
        container.setBeanIdResolver(new BeanIdResolver<AgentDeployment, AgentDeployment>() {
            @Override
            public AgentDeployment getIdForBean(AgentDeployment bean) {
                return bean;
            }
        });       

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns(new Object[] { "name", "status", "logLevel", "startType",
                "startExpression" });
        table.addItemClickListener(new TableItemClickListener());
        table.addValueChangeListener(new TableValueChangeListener());
        table.setTableFieldFactory(new EditableFieldFactory());

        addComponent(table);
        setExpandRatio(table, 1.0f);

        container.addAll(agent.getAgentDeployments());

        if (agent.getAgentDeployments().size() > 0) {
            table.setValue(agent.getAgentDeployments().iterator().next());
            table.focus();
        } else {
            addDeploymentButton.focus();
        }

        setButtonsEnabled();
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
    }
    
    @Override
    public void deselected() {
    }

    protected void setButtonsEnabled() {
        boolean selected = getSelectedItems().size() > 0;
        removeButton.setEnabled(selected);
        editButton.setEnabled(selected);
    }

    @SuppressWarnings("unchecked")
    protected Set<Object> getSelectedItems() {
        return (Set<Object>) table.getValue();
    }

    protected void selectOnly(Object itemId) {
        for (Object id : getSelectedItems()) {
            table.unselect(id);
        }
        table.select(itemId);
    }

    protected void editSelectedItem() {
        lastEditItemIds = getSelectedItems();
        table.setEditable(true);
    }

    class EditableFieldFactory extends DefaultFieldFactory {

        @Override
        public Field<?> createField(Container container, Object itemId, Object propertyId,
                Component uiContext) {
            if (lastEditItemIds.contains(itemId)) {
                AgentDeployment deployment = (AgentDeployment)itemId;
                if (propertyId.equals("flow")) {                    
                    AbstractSelect combo = new ComboBox();
                    combo.setWidth(100, Unit.PERCENTAGE);
                    IConfigurationService service = context.getConfigurationService();
                    List<Flow> allFlows = new ArrayList<Flow>(service.findFlows());
                    @SuppressWarnings("unchecked")
                    Collection<AgentDeployment> allDeployments = (Collection<AgentDeployment>) container
                            .getItemIds();
                    for (AgentDeployment agentDeployment : allDeployments) {
                        Iterator<Flow> i = allFlows.iterator();
                        while (i.hasNext()) {
                            Flow flow = i.next();
                            if (agentDeployment.getFlow().equals(flow)) {
                                i.remove();
                            }
                        }
                    }
                    combo.addItems(allFlows);
                    if (!combo.getItemIds().contains(deployment.getFlow())) {
                        combo.addItem(deployment.getFlow());
                        combo.setValue(deployment.getFlow());
                    }

                    combo.focus();
                    return combo;
                } else if (propertyId.equals("logLevel")) {
                    AbstractSelect combo = new ComboBox();
                    combo.setWidth(100, Unit.PERCENTAGE);
                    LogLevel[] levels = LogLevel.values();
                    for (LogLevel logLevel : levels) {
                        combo.addItem(logLevel.name());
                    }
                    combo.setValue(deployment.getLogLevel());
                    return combo;
                } else if (propertyId.equals("startType")) {
                    AbstractSelect combo = new ComboBox();
                    combo.setWidth(100, Unit.PERCENTAGE);
                    StartType[] values = StartType.values();
                    for (StartType value : values) {
                        combo.addItem(value.name());
                    }
                    combo.setValue(deployment.getStartType());
                    return combo;
                } else if (propertyId.equals("startExpression")) {
                    TextField field = new TextField();
                    field.setWidth(100, Unit.PERCENTAGE);
                    field.setNullRepresentation("");
                    return field;
                } else {
                    super.createField(container, itemId, propertyId, uiContext);
                }
            }
            return null;
        }

    }

    class AddDeploymentClickListener implements ClickListener, FlowSelectListener {
        public void buttonClick(ClickEvent event) {
            if (flowSelectWindow == null) {
                String introText = "Select one or more flows for deployment to this agent.";
                flowSelectWindow = new FlowSelectWindow(context, "Add Deployment", introText);
                flowSelectWindow.setFlowSelectListener(this);
            }
            UI.getCurrent().addWindow(flowSelectWindow);
        }
        
        public void selected(Collection<Flow> flowCollection) {
            for (Flow flow : flowCollection) {
                AgentDeployment deployment = new AgentDeployment();
                deployment.setAgentId(agent.getId());
                deployment.setFlow(flow);
                deployment.setName(getName(flow.getName()));
                container.addItem(deployment, deployment);
                context.getConfigurationService().save(deployment);
            }
        }

        protected String getName(String name) {
            for (AgentDeployment deployment : container.getItemIds()) {
                if (name.equals(deployment.getName())) {
                    if (name.matches(".*\\([0-9]+\\)$")) {
                        String num = name.substring(name.lastIndexOf("(") + 1, name.lastIndexOf(")"));
                        name = name.replaceAll("\\([0-9]+\\)$", "(" + (Integer.parseInt(num) + 1) + ")");
                    } else {
                        name += " (1)";
                    }
                    return getName(name); 
                }
            }
            return name;
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            AgentDeployment deployment = (AgentDeployment) getSelectedItems().iterator().next();
            EditAgentDeploymentPanel editPanel = new EditAgentDeploymentPanel(context, deployment);
            tabbedPanel.addCloseableTab(deployment.getId(), deployment.getName(), Icons.DEPLOYMENT, editPanel);
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<Object> selectedIds = getSelectedItems();
            for (Object itemId : selectedIds) {
                table.removeItem(itemId);
                context.getConfigurationService().delete((AgentDeployment) itemId);
            }
        }
    }

    class TableItemClickListener implements ItemClickListener {
        long lastClick;
        
        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                table.setValue(event.getItemId());
                editSelectedItem();
            } else {
                if (getSelectedItems().contains(event.getItemId()) &&
                        System.currentTimeMillis()-lastClick > 500) {
                    table.setValue(null);
                }
            }
            
            lastClick = System.currentTimeMillis();
        }
    }

    class TableValueChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            for (Object itemId : lastEditItemIds) {
                if (itemId instanceof AgentDeployment) {
                    AgentDeployment agentDeployment = (AgentDeployment) itemId;
                    try {
                        context.getConfigurationService().save(agentDeployment);
                        agent.getAgentDeployments().add(agentDeployment);
                    } catch (Exception e1) {
                        table.removeItem(agentDeployment);
                    }
                }
            }
            table.setEditable(false);
            setButtonsEnabled();
        }
    }

}
