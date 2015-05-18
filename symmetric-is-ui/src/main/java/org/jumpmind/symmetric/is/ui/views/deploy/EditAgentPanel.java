package org.jumpmind.symmetric.is.ui.views.deploy;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.builder.CompareToBuilder;
import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentParameter;
import org.jumpmind.symmetric.is.core.model.AgentDeploymentSummary;
import org.jumpmind.symmetric.is.core.model.AgentResource;
import org.jumpmind.symmetric.is.core.model.AgentStartMode;
import org.jumpmind.symmetric.is.core.model.DeploymentStatus;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowName;
import org.jumpmind.symmetric.is.core.model.FlowParameter;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.is.core.runtime.resource.Datasource;
import org.jumpmind.symmetric.is.core.runtime.resource.LocalFile;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.common.IBackgroundRefreshable;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.is.ui.init.BackgroundRefresherService;
import org.jumpmind.symmetric.is.ui.views.manage.ExecutionLogPanel;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.util.AppUtils;

import com.vaadin.data.Container.Sortable;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItem;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.data.util.DefaultItemSorter;
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
public class EditAgentPanel extends VerticalLayout implements IUiPanel, IBackgroundRefreshable, AgentDeploymentChangeListener {

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    Agent agent;

    Table table;

    BeanItemContainer<AgentDeploymentSummary> container;

    Button addDeploymentButton;

    Button enableButton;

    Button disableButton;

    Button removeButton;

    Button editButton;    
    
    Button runButton;

    FlowSelectWindow flowSelectWindow;
    
    BackgroundRefresherService backgroundRefresherService;

    public EditAgentPanel(ApplicationContext context, TabbedPanel tabbedPanel, Agent agent) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;
        this.agent = agent;
        this.backgroundRefresherService = context.getBackgroundRefresherService();
        
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
        
        Button parameterButton = new Button("Parameters");
        parameterButton.addClickListener(new ParameterClickListener());
        editAgentLayout.addComponent(parameterButton);
        editAgentLayout.setComponentAlignment(parameterButton, Alignment.BOTTOM_LEFT);
        
        HorizontalLayout buttonGroup = new HorizontalLayout();
        
        final TextField hostNameField = new TextField("Hostname");
        hostNameField.setImmediate(true);
        hostNameField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        hostNameField.setTextChangeTimeout(100);
        hostNameField.setWidth(20, Unit.EM);
        hostNameField.setNullRepresentation("");
        hostNameField.setValue(agent.getHost());
        hostNameField.addValueChangeListener(new ValueChangeListener() {
            public void valueChange(ValueChangeEvent event) {
                Agent agent = EditAgentPanel.this.agent; 
                agent.setHost((String)hostNameField.getValue());
                EditAgentPanel.this.context.getConfigurationService().save((AbstractObject)agent);   
                EditAgentPanel.this.context.getAgentManager().refresh(agent);
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

        addDeploymentButton = buttonBar.addButton("Add Deployment", Icons.DEPLOYMENT);
        addDeploymentButton.addClickListener(new AddDeploymentClickListener());

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());

        enableButton = buttonBar.addButton("Enable", FontAwesome.CHAIN);
        enableButton.addClickListener(new EnableClickListener());

        disableButton = buttonBar.addButton("Disable", FontAwesome.CHAIN_BROKEN);
        disableButton.addClickListener(new DisableClickListener());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new RemoveClickListener());
        
        runButton = buttonBar.addButton("Run", Icons.RUN);
        runButton.addClickListener(new RunClickListener());

        container = new BeanItemContainer<AgentDeploymentSummary>(AgentDeploymentSummary.class);
        container.setItemSorter(new TableItemSorter());

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
        table.setSortContainerPropertyId("projectName");
        table.setSortAscending(true);

        addComponent(table);
        setExpandRatio(table, 1.0f);
        refresh();
        setButtonsEnabled();
        backgroundRefresherService.register(this);
    }

    @Override
    public boolean closing() {
        backgroundRefresherService.unregister(this);
        return true;
    }

    @Override
    public void selected() {
    }
    
    @Override
    public void deselected() {
    }

    public void changed(AgentDeployment agentDeployment) {
        for (AgentDeploymentSummary summary : container.getItemIds()) {
            if (summary.getId().equals(agentDeployment.getId())) {
                summary.copy(agentDeployment);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object onBackgroundDataRefresh() {
        return getRefreshData();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
        updateItems((List<AgentDeploymentSummary>) backgroundData);
    }

    protected List<AgentDeploymentSummary> getRefreshData() {
        return context.getConfigurationService().findAgentDeploymentSummary(agent.getId());
    }

    public void refresh() {
        updateItems(getRefreshData());
    }
    
    protected void updateItem(AgentDeploymentSummary summary) {
        Set<AgentDeploymentSummary> selectedItems = getSelectedItems();
        container.removeItem(summary);
        container.addItem(summary);
        table.sort();
        setSelectedItems(selectedItems);
        setButtonsEnabled();
    }

    protected void updateItems(List<AgentDeploymentSummary> summaries) {
        boolean isChanged = false;
        Set<AgentDeploymentSummary> selectedItems = getSelectedItems();
        for (AgentDeploymentSummary summary : summaries) {
            BeanItem<AgentDeploymentSummary> beanItem = container.getItem(summary);
            if (beanItem == null || beanItem.getBean().isChanged(summary)) {
                container.removeItem(summary);
                container.addItem(summary);
                isChanged = true;
            }
        }
        Set<AgentDeploymentSummary> items = new HashSet<AgentDeploymentSummary>(container.getItemIds());
        for (AgentDeploymentSummary summary : items) {
            if (!summaries.contains(summary)) {
                container.removeItem(summary);
                isChanged = true;
            }
        }
        if (isChanged) {
            table.sort();
            setSelectedItems(selectedItems);
            setButtonsEnabled();
        }
    }

    protected void setButtonsEnabled() {
        boolean canRemove = false;
        boolean canEnable = false;
        boolean canDisable = false;
        boolean canRun = false;
        Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
        for (AgentDeploymentSummary summary : selectedIds) {
            if (summary.isFlow()) {
                if (summary.getStatus().equals(DeploymentStatus.DEPLOYED.name()) || 
                        summary.getStatus().equals(DeploymentStatus.DISABLED.name())) {
                    canRemove = true;
                }
                if (summary.getStatus().equals(DeploymentStatus.DEPLOYED.name())) {
                    canDisable = true;
                    if (summary.isFlow()) {
                       canRun = true;
                    }
                }
                if (summary.getStatus().equals(DeploymentStatus.DISABLED.name())) {
                    canEnable = true;
                }
            }
        }
        runButton.setEnabled(canRun && selectedIds.size() == 1); 
        enableButton.setEnabled(canEnable);
        disableButton.setEnabled(canDisable);
        removeButton.setEnabled(canRemove);
        editButton.setEnabled(getSelectedItems().size() > 0);
    }

    @SuppressWarnings("unchecked")
    protected Set<AgentDeploymentSummary> getSelectedItems() {
        return (Set<AgentDeploymentSummary>) table.getValue();
    }

    protected void setSelectedItems(Set<AgentDeploymentSummary> selectedItems) {
        table.setValue(null);
        for (AgentDeploymentSummary summary : selectedItems) {
            BeanItem<AgentDeploymentSummary> beanItem = container.getItem(summary);
            if (beanItem != null) {
                AgentDeploymentSummary updatedSummary = beanItem.getBean();
                table.select(updatedSummary);
            }
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
        
        public void selected(Collection<FlowName> flowCollection) {
            for (FlowName flowName : flowCollection) {
                Flow flow = context.getConfigurationService().findFlow(flowName.getId());
                AgentDeployment deployment = new AgentDeployment();
                deployment.setAgentId(agent.getId());
                deployment.setFlow(flow);
                deployment.setName(getName(flow.getName()));
                List<AgentDeploymentParameter> deployParams = deployment.getAgentDeploymentParameters();
                for (FlowParameter flowParam : flow.getFlowParameters()) {
                    AgentDeploymentParameter deployParam = new AgentDeploymentParameter();
                    deployParam.setFlowParameterId(flowParam.getId());
                    deployParam.setAgentDeploymentId(deployment.getId());
                    deployParam.setName(flowParam.getName());
                    deployParam.setValue(flowParam.getDefaultValue());
                    deployParams.add(deployParam);
                }
                context.getConfigurationService().save(deployment);
            }
            refresh();
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
    
    class RunClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            AgentDeploymentSummary summary = (AgentDeploymentSummary) getSelectedItems().iterator().next();
            if (summary.isFlow()) {
                AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                IAgentManager agentManager = context.getAgentManager();
                String executionId = agentManager.getAgentRuntime(deployment.getAgentId()).scheduleNow(deployment);
                if (executionId != null) {
                    ExecutionLogPanel logPanel = new ExecutionLogPanel(executionId, context);
                    tabbedPanel.addCloseableTab(executionId, "Run " + deployment.getName(), Icons.LOG, logPanel);
                }
            }
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            AgentDeploymentSummary summary = (AgentDeploymentSummary) getSelectedItems().iterator().next();
            if (summary.isFlow()) {
                AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                EditAgentDeploymentPanel editPanel = new EditAgentDeploymentPanel(context, deployment, EditAgentPanel.this);
                tabbedPanel.addCloseableTab(deployment.getId(), deployment.getName(), Icons.DEPLOYMENT, editPanel);
            } else {
                AgentResource agentResource = context.getConfigurationService().findAgentResource(agent.getId(), summary.getId());
                EditAgentResourcePanel editPanel = new EditAgentResourcePanel(context, agentResource);
                FontAwesome icon = Icons.GENERAL_RESOURCE;
                if (agentResource.getType().equals(Datasource.TYPE)) {
                    icon = Icons.DATABASE;
                } else if (agentResource.getType().equals(LocalFile.TYPE)) {
                    icon = Icons.FILE_SYSTEM;
                }
                tabbedPanel.addCloseableTab(summary.getId(), summary.getName(), icon, editPanel);
            }
        }
    }

    class EnableClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
            for (AgentDeploymentSummary summary : selectedIds) {
                if (summary.isFlow()) {
                    AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                    deployment.setStatus(DeploymentStatus.REQUEST_DEPLOY.name());
                    summary.setStatus(DeploymentStatus.REQUEST_DEPLOY.name());
                    context.getConfigurationService().save(deployment);
                    updateItem(summary);
                }
            }
        }
    }

    class DisableClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
            for (AgentDeploymentSummary summary : selectedIds) {
                if (summary.isFlow()) {
                    AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                    deployment.setStatus(DeploymentStatus.REQUEST_DISABLE.name());
                    summary.setStatus(DeploymentStatus.REQUEST_DISABLE.name());
                    context.getConfigurationService().save(deployment);
                    updateItem(summary);
                }
            }
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<AgentDeploymentSummary> selectedIds = getSelectedItems();
            for (AgentDeploymentSummary summary : selectedIds) {
                if (summary.isFlow()) {
                    AgentDeployment deployment = context.getConfigurationService().findAgentDeployment(summary.getId());
                    deployment.setStatus(DeploymentStatus.REQUEST_UNDEPLOY.name());
                    summary.setStatus(DeploymentStatus.REQUEST_UNDEPLOY.name());
                    context.getConfigurationService().save(deployment);
                    updateItem(summary);
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
            setButtonsEnabled();
        }
    }

    class TableItemSorter extends DefaultItemSorter {
        Object[] propertyId;
        
        boolean[] ascending;
        
        public void setSortProperties(Sortable container, Object[] propertyId, boolean[] ascending) {
            super.setSortProperties(container, propertyId, ascending);
            this.propertyId = propertyId;
            this.ascending = ascending;
        }

        public int compare(Object o1, Object o2) {
            AgentDeploymentSummary s1 = (AgentDeploymentSummary) o1;
            AgentDeploymentSummary s2 = (AgentDeploymentSummary) o2;
            if (propertyId != null && propertyId.length > 0 && propertyId[0].equals("projectName")) {
                return new CompareToBuilder().append(s1.getProjectName(), s2.getProjectName())
                        .append(s1.getName(), s2.getName()).toComparison() * (ascending[0] ? 1 : -1);
            }
            return super.compare(o1, o2);
        }        
    }
    
    class ParameterClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            EditAgentParametersWindow window = new EditAgentParametersWindow(context, agent);
            window.showAtSize(0.5);
        }
    }

}
