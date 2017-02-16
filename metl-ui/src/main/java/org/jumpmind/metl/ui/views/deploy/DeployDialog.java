package org.jumpmind.metl.ui.views.deploy;

import java.util.Collection;
import java.util.List;

import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.AgentDeploymentParameter;
import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.Page;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class DeployDialog extends ResizableWindow {

    private static final String DEPLOY_BY_FLOW = "By Flow";

    private static final String DEPLOY_BY_PACKAGE = "By Package";

    private static final long serialVersionUID = 1L;

    ApplicationContext context;

    EditAgentPanel parentPanel;

    VerticalLayout selectDeploymentLayout;

    OptionGroup deployByOptionGroup;

    Button actionButton;

    Button backButton;

    SelectFlowsPanel selectFlowsPanel;
    
    SelectPackagePanel selectPackagePanel;
    
    ValidateReleasePackageDeploymentPanel validateReleasePackageDeploymentPanel;

    public DeployDialog(ApplicationContext context, EditAgentPanel parentPanel) {
        super("Deploy");
        this.parentPanel = parentPanel;
        this.context = context;

        final float DESIRED_WIDTH = 600;
        float width = DESIRED_WIDTH;
        float maxWidth = (float) (Page.getCurrent().getBrowserWindowWidth() * .8);
        if (maxWidth < DESIRED_WIDTH) {
            width = maxWidth;
        }
        setWidth(width, Unit.PIXELS);
        setHeight(600.0f, Unit.PIXELS);

        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSizeFull();
        addComponent(layout, 1);

        deployByOptionGroup = new OptionGroup("Deployment Type:");
        deployByOptionGroup.addStyleName(ValoTheme.OPTIONGROUP_HORIZONTAL);
        deployByOptionGroup.addItem(DEPLOY_BY_PACKAGE);
        deployByOptionGroup.addItem(DEPLOY_BY_FLOW);
        layout.addComponent(deployByOptionGroup);

        selectDeploymentLayout = new VerticalLayout();
        selectDeploymentLayout.setSizeFull();
        selectDeploymentLayout.setMargin(new MarginInfo(true, false));
        layout.addComponent(selectDeploymentLayout);
        layout.setExpandRatio(selectDeploymentLayout, 1);

        backButton = new Button("Cancel", e -> back());
        actionButton = new Button("Deploy", e -> takeAction());
        actionButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        actionButton.setClickShortcut(KeyCode.ENTER);
        addComponent(buildButtonFooter(backButton, actionButton));

        deployByOptionGroup.addValueChangeListener(e -> deployByChanged());
        deployByOptionGroup.setValue(DEPLOY_BY_PACKAGE);

    }

    protected boolean isDeployByFlow() {
        Object deployByChoice = deployByOptionGroup.getValue();
        return deployByOptionGroup.isVisible() && DEPLOY_BY_FLOW.equals(deployByChoice);
    }

    protected boolean isDeployByPackage() {
        Object deployByChoice = deployByOptionGroup.getValue();
        return deployByOptionGroup.isVisible() && DEPLOY_BY_PACKAGE.equals(deployByChoice);
    }

    protected void deployByChanged() {
        selectDeploymentLayout.removeAllComponents();
        Component toAdd = null;
        if (isDeployByFlow()) {
            toAdd = buildDeployByFlow();
        } else {
            toAdd = buildDeployByPackage();
        }
        selectDeploymentLayout.addComponent(toAdd);
    }

    protected Component buildDeployByFlow() {
        if (selectFlowsPanel == null) {
            String introText = "Select one or more flows for deployment to this agent:";
            selectFlowsPanel = new SelectFlowsPanel(context, introText, parentPanel.getAgent().isAllowTestFlows());
        }
        actionButton.setCaption("Deploy");
        return selectFlowsPanel;
    }

    protected Component buildDeployByPackage() {
        if (selectPackagePanel == null) {
            String introText = "Select a package for deployment to this agent:";
            selectPackagePanel = new SelectPackagePanel(context, introText);            
        }
        actionButton.setCaption("Next");
        return selectPackagePanel;        
    }
    
    protected Component buildValidatePackageDeploymentAction() {
        if (validateReleasePackageDeploymentPanel == null) {
            String introText = "Validate deployment actions";
            validateReleasePackageDeploymentPanel = new ValidateReleasePackageDeploymentPanel(context, introText);
        }
        return validateReleasePackageDeploymentPanel;
    }

    protected void takeAction() {
        if (isDeployByFlow()) {
            Collection<FlowName> flows = selectFlowsPanel.getSelectedFlows(parentPanel.getAgent().isAllowTestFlows());
            verfiyDeployFlows(flows);
        } else if (isDeployByPackage()) {
            deployByOptionGroup.setVisible(false);
            backButton.setCaption("Previous");
            actionButton.setCaption("Deploy");
            selectDeploymentLayout.removeAllComponents();
            selectDeploymentLayout.addComponent(buildValidatePackageDeploymentAction());
        } else {
            log.info("Do package deployment now!");
        }
    }

    protected void back() {
        if (deployByOptionGroup.isVisible()) {
            close();
        } else {
            deployByOptionGroup.setVisible(true);
            backButton.setCaption("Cancel");
            deployByChanged();
        }
    }

    protected void verfiyDeployFlows(Collection<FlowName> flowCollection) {
        StringBuilder alreadyDeployedFlows = new StringBuilder();
        List<AgentDeploymentSummary> summaries = parentPanel.getAgentDeploymentSummary();
        for (FlowName flowName : flowCollection) {
            for (AgentDeploymentSummary agentDeploymentSummary : summaries) {
                if (flowName.getId().equals(agentDeploymentSummary.getArtifactId())) {
                    if (alreadyDeployedFlows.length() > 0) {
                        alreadyDeployedFlows.append(", ");
                    }
                    alreadyDeployedFlows.append("'").append(flowName.getName()).append("'");
                }
            }
        }

        if (alreadyDeployedFlows.length() > 0) {
            ConfirmDialog.show("Flows already deployed",
                    String.format(
                            "There are flows that have already been deployed.  Press OK to deploy another version. The following flows are already deployed: %s",
                            alreadyDeployedFlows),
                    () -> {
                        deployFlows(flowCollection);
                        return true;
                    });
        } else {
            deployFlows(flowCollection);
        }
    }

    protected void deployFlows(Collection<FlowName> flowCollection) {
        for (FlowName flowName : flowCollection) {
            IConfigurationService configurationService = context.getConfigurationService();
            Flow flow = configurationService.findFlow(flowName.getId());
            AgentDeployment deployment = new AgentDeployment();
            deployment.setAgentId(parentPanel.getAgent().getId());
            deployment.setName(getName(flow.getName()));
            deployment.setFlowId(flow.getId());
            List<AgentDeploymentParameter> deployParams = deployment.getAgentDeploymentParameters();
            for (FlowParameter flowParam : flow.getFlowParameters()) {
                AgentDeploymentParameter deployParam = new AgentDeploymentParameter();
                deployParam.setFlowParameterId(flowParam.getId());
                deployParam.setAgentDeploymentId(deployment.getId());
                deployParam.setName(flowParam.getName());
                deployParam.setValue(flowParam.getDefaultValue());
                deployParams.add(deployParam);
            }
            context.getOperationsSerivce().save(deployment);
        }
        parentPanel.refresh();
        close();

    }

    protected String getName(String name) {
        for (Object deployment : parentPanel.getAgentDeploymentSummary()) {
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
