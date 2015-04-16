package org.jumpmind.symmetric.is.ui.views.deploy;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentParameter;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentSummary;
import org.jumpmind.symmetric.is.core.model.AgentResource;
import org.jumpmind.symmetric.is.core.model.AgentStartMode;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowParameter;
import org.jumpmind.symmetric.is.core.runtime.resource.DataSourceResource;
import org.jumpmind.symmetric.is.core.runtime.resource.LocalFileResource;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.util.AppUtils;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditAgentPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    Agent agent;

    Table table;

    BeanItemContainer<AgentDeploymentSummary> container;

    Button removeButton;

    Button editButton;    

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
            public void valueChange(ValueChangeEvent event) {
                EditAgentPanel.this.agent.setHost((String)hostNameField.getValue());
                EditAgentPanel.this.context.getConfigurationService().save((AbstractObject)EditAgentPanel.this.agent);                
            }
        });
        buttonGroup.addComponent(hostNameField);
        buttonGroup.setComponentAlignment(hostNameField, Alignment.BOTTOM_LEFT);
        
        Button getHostNameButton = new Button("Get Host");
        getHostNameButton.addClickListener(new ClickListener() {
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

        container = new BeanItemContainer<AgentDeploymentSummary>(AgentDeploymentSummary.class);

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns("projectName", "name", "type", "status", "logLevel", "startType", "startExpression");
        table.setColumnHeaders("Project Name", "Deployment", "Type", "Status", "Log Level", "Start Type", "Start Expression");
        table.addItemClickListener(new TableItemClickListener());
        table.addValueChangeListener(new TableValueChangeListener());

        addComponent(table);
        setExpandRatio(table, 1.0f);

        container.addAll(context.getConfigurationService().findAgentDeploymentSummary(agent.getId()));

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
        boolean removable = false;
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                removable = true;
            }
        }
        removeButton.setEnabled(removable);
        editButton.setEnabled(selected);
    }

    @SuppressWarnings("unchecked")
    protected Set<AgentDeploymentSummary> getSelectedItems() {
        return (Set<AgentDeploymentSummary>) table.getValue();
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
                List<AgentDeploymentParameter> deployParams = deployment.getAgentDeploymentParameters();
                for (FlowParameter flowParam : flow.getFlowParameters()) {
                    AgentDeploymentParameter deployParam = new AgentDeploymentParameter();
                    deployParam.setId(flowParam.getId());
                    deployParam.setAgentDeploymentId(deployment.getId());
                    deployParam.setName(flowParam.getName());
                    deployParam.setValue(flowParam.getDefaultValue());
                    deployParams.add(deployParam);
                }
                context.getConfigurationService().save(deployment);
                container.addItem(deployment);
            }
        }

        protected String getName(String name) {
            for (Object deployment : container.getItemIds()) {
                if (deployment instanceof AgentDeployment) {
                    AgentDeployment agentDeployment = (AgentDeployment) deployment;
                    if (name.equals(agentDeployment.getName())) {
                        if (name.matches(".*\\([0-9]+\\)$")) {
                            String num = name.substring(name.lastIndexOf("(") + 1, name.lastIndexOf(")"));
                            name = name.replaceAll("\\([0-9]+\\)$", "(" + (Integer.parseInt(num) + 1) + ")");
                        } else {
                            name += " (1)";
                        }
                        return getName(name); 
                    }                    
                }
            }
            return name;
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            AgentDeploymentSummary summary = (AgentDeploymentSummary) getSelectedItems().iterator().next();
            if (summary.isFlow()) {
                AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                EditAgentDeploymentPanel editPanel = new EditAgentDeploymentPanel(context, deployment);
                tabbedPanel.addCloseableTab(deployment.getId(), deployment.getName(), Icons.DEPLOYMENT, editPanel);
            } else {
                AgentResource agentResource = context.getConfigurationService().findAgentResource(agent.getId(), summary.getId());
                EditAgentResourcePanel editPanel = new EditAgentResourcePanel(context, agentResource);
                FontAwesome icon = Icons.GENERAL_RESOURCE;
                if (agentResource.getType().equals(DataSourceResource.TYPE)) {
                    icon = Icons.DATABASE;
                } else if (agentResource.getType().equals(LocalFileResource.TYPE)) {
                    icon = Icons.FILE_SYSTEM;
                }
                tabbedPanel.addCloseableTab(summary.getId(), summary.getName(), icon, editPanel);
            }
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
            for (AgentDeploymentSummary summary : selectedIds) {
                if (summary.isFlow()) {
                    AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                    context.getConfigurationService().delete(deployment);
                    table.removeItem(summary);
                    // TODO: remove Resources that are no longer referenced
                }
            }
        }
    }

    class TableItemClickListener implements ItemClickListener {
        long lastClick;
        
        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                editButton.click();
            } else if (getSelectedItems().contains(event.getItemId()) &&
                System.currentTimeMillis()-lastClick > 500) {
                    table.setValue(null);
            }
            lastClick = System.currentTimeMillis();
        }
    }

    class TableValueChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            table.setEditable(false);
            setButtonsEnabled();
        }
    }

}
