package org.jumpmind.symmetric.is.ui.views.deprecated;

import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentSummary;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
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
import com.vaadin.ui.Component;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope(value = "ui")
public class EditFlowDeploymentsWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    SelectAgentsWindow selectAgentsWindow;
    
    @Autowired
    IAgentManager agentManager;

    FlowVersion componentFlowVersion;

    BeanItemContainer<AgentDeployment> container;

    MenuItem undeployButton;

    MenuItem deployButton;
    
    MultiSelectTable table;

    public EditFlowDeploymentsWindow() {
        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        Component comp = buildMainLayout();
        content.addComponent(comp);
        content.setExpandRatio(comp, 1);

        Button closeButton = new Button("Close", new CloseButtonListener());
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

    public void show(FlowVersion componentFlowVersion) {
        this.componentFlowVersion = componentFlowVersion;
        setCaption("Agent Deployments for '" + componentFlowVersion.getName() + "'");
        container.removeAllItems();        
        List<AgentDeployment> deployments = configurationService.findAgentDeploymentsFor(componentFlowVersion);
        container.addAll(deployments);
        showAtSize(.6);
    }

    public FlowVersion getComponentFlowVersion() {
        return componentFlowVersion;
    }

    class DeployCommand implements Command, IItemUpdatedListener {
        private static final long serialVersionUID = 1L;

        @Override
        public void menuSelected(MenuItem selectedItem) {
            selectAgentsWindow.show(componentFlowVersion.getId(), this);
        }

        @Override
        public void itemUpdated(Object item) {
            if (item instanceof Set) {
                @SuppressWarnings("unchecked")
                Set<AgentSummary> selected = (Set<AgentSummary>) item;
                for (AgentSummary summary : selected) {
                    container.addBean(agentManager.deploy(summary.getId(), componentFlowVersion));
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
                agentManager.undeploy(agentDeployment);
                container.removeItem(agentDeployment);
            }
        }
    }



}
