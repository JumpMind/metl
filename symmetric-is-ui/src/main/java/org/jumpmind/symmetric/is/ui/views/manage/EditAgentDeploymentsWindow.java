package org.jumpmind.symmetric.is.ui.views.manage;

import java.util.Set;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.config.AgentDeployment;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersionSummary;
import org.jumpmind.symmetric.is.core.config.data.AgentDeploymentData;
import org.jumpmind.symmetric.is.core.config.data.ComponentFlowVersionData;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.ui.common.IItemUpdatedListener;
import org.jumpmind.symmetric.ui.common.MultiSelectTable;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
public class EditAgentDeploymentsWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    SelectComponentFlowVersionWindow selectComponentFlowVersionWindow;

    IItemUpdatedListener itemUpdatedListener;

    Agent agent;

    BeanItemContainer<AgentDeployment> container;

    MenuItem undeployButton;

    MenuItem deployButton;
    
    MultiSelectTable table;

    public EditAgentDeploymentsWindow() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        Component comp = buildMainLayout();
        content.addComponent(comp);
        content.setExpandRatio(comp, 1);

        Button closeButton = new Button("Close", new CloseClickListener());
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);

        content.addComponent(buildButtonFooter(new Button[0], new Button[] { closeButton }));

    }

    protected VerticalLayout buildMainLayout() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSpacing(true);
        layout.setSizeFull();

        MenuBar menuBar = new MenuBar();
        layout.addComponent(menuBar);
        deployButton = menuBar.addItem("Deploy...", FontAwesome.DOWNLOAD, new DeployCommand());
        undeployButton = menuBar.addItem("Undeploy", FontAwesome.UPLOAD, new UnDeployCommand());
        undeployButton.setEnabled(false);

        table = new MultiSelectTable();
        table.addValueChangeListener(new ValueChangeListener() {
            
            private static final long serialVersionUID = 1L;

            @Override
            public void valueChange(ValueChangeEvent event) {
                @SuppressWarnings("unchecked")
                Set<AgentDeployment> deployments = (Set<AgentDeployment>)table.getValue();
                undeployButton.setEnabled(deployments != null && deployments.size() > 0);
            }
        });
        container = new BeanItemContainer<AgentDeployment>(AgentDeployment.class);
        table.setContainerDataSource(container);
        table.setSizeFull();

        table.setColumnHeader("name", "Name");
        table.setColumnHeader("status", "Status");
        table.setColumnHeader("message", "Message");
        table.setColumnHeader("createTime", "Time Deployed");

        table.setVisibleColumns("name", "status", "message", "createTime");        

        layout.addComponent(table);
        layout.setExpandRatio(table, 1);

        return layout;
    }

    public void show(Agent agent, IItemUpdatedListener itemUpdatedListener) {
        this.agent = agent;
        this.itemUpdatedListener = itemUpdatedListener;
        setCaption("Agent Deployments for '" + agent.toString() + "'");
        container.removeAllItems();
        container.addAll(agent.getAgentDeployments());
        showAtSize(.6);
    }

    public Agent getAgent() {
        return agent;
    }

    protected void done() {
        itemUpdatedListener.itemUpdated(agent);
        close();
    }

    class DeployCommand implements Command, IItemUpdatedListener {
        private static final long serialVersionUID = 1L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            selectComponentFlowVersionWindow.show(agent, this);
        }

        @Override
        public void itemUpdated(Object item) {
            if (item instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<ComponentFlowVersionSummary> selectedFlows = (Set<ComponentFlowVersionSummary>) item;
                for (ComponentFlowVersionSummary componentFlowVersionSummary : selectedFlows) {
                    AgentDeploymentData data = new AgentDeploymentData();
                    data.setAgentId(agent.getData().getId());
                    data.setComponentFlowVersionId(componentFlowVersionSummary.getId());
                    ComponentFlowVersion componentFlowVersion = new ComponentFlowVersion(null,
                            new ComponentFlowVersionData(componentFlowVersionSummary.getId()));
                    configurationService.refresh(componentFlowVersion);
                    AgentDeployment agentDeployment = new AgentDeployment(componentFlowVersion, data);
                    configurationService.save(agentDeployment);
                    agent.getAgentDeployments().add(agentDeployment);
                    container.addBean(agentDeployment);
                }
            }
        }
    }

    class UnDeployCommand implements Command {
        private static final long serialVersionUID = 1L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            Set<AgentDeployment> deploymentsSelected = table.getSelected();
            for (AgentDeployment agentDeployment : deploymentsSelected) {
                configurationService.delete(agentDeployment);
                agent.getAgentDeployments().remove(agentDeployment);
                container.removeItem(agentDeployment);
            }
        }
    }

    class CloseClickListener implements ClickListener {
        private static final long serialVersionUID = 1L;

        @Override
        public void buttonClick(ClickEvent event) {
            done();
        }

    }

}
