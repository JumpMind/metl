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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.AgentFlowDeployParm;
import org.jumpmind.metl.core.model.AgentResourceSetting;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowParameter;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.Rppv;
import org.jumpmind.metl.core.model.ResourceName;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.views.deploy.ValidateFlowDeploymentPanel.DeploymentLine;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.data.util.BeanItemContainer;
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
    
    IConfigurationService configurationService;
    
    IOperationsService operationsService;

    EditAgentPanel parentPanel;

    VerticalLayout selectDeploymentLayout;

    OptionGroup deployByOptionGroup;

    Button actionButton;

    Button backButton;

    SelectFlowsPanel selectFlowsPanel;
    
    SelectPackagePanel selectPackagePanel;
    
    ValidateFlowDeploymentPanel validateFlowDeploymentPanel;

    public DeployDialog(ApplicationContext context, EditAgentPanel parentPanel) {
        super("Deploy");
        this.context = context;
        this.configurationService = context.getConfigurationService();
        this.operationsService = context.getOperationsService();
        this.parentPanel = parentPanel;
        this.context = context;

        final float DESIRED_WIDTH = 1000;
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
        actionButton.setCaption("Next");
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
    
    protected List<FlowName> getFlowsFromReleasePackages(List<ReleasePackage> releasePackages) {
        List<FlowName> flows = new ArrayList<FlowName>();
        
        for (ReleasePackage releasePackage : releasePackages) {
            List<Rppv> rppvs = configurationService.findReleasePackageProjectVersions(releasePackage.getId());
            for (Rppv rppv : rppvs) {
                flows.addAll(configurationService.findFlowsInProject(rppv.getProjectVersionId(), false));
            }                    
        }        
        return flows;
    }
    
    protected void takeAction() {
        if (isDeployByFlow() || isDeployByPackage()) {
            if (validateFlowDeploymentPanel == null) {
                String introText = "Validate deployment actions";
                if (isDeployByPackage()) {
                    validateFlowDeploymentPanel = new ValidateFlowDeploymentPanel(
                            context, introText, getFlowsFromReleasePackages(selectPackagePanel.getSelectedPackages()),
                            parentPanel.getAgent().getId());
                } else {
                    List<FlowName> flows = new ArrayList<FlowName>(selectFlowsPanel.getSelectedFlows(parentPanel.getAgent().isAllowTestFlows()));
                    validateFlowDeploymentPanel = new ValidateFlowDeploymentPanel(
                            context, introText, flows,
                            parentPanel.getAgent().getId());                    
                }
            }
            deployByOptionGroup.setVisible(false);
            backButton.setCaption("Previous");
            actionButton.setCaption("Deploy");
            selectDeploymentLayout.removeAllComponents();
            selectDeploymentLayout.addComponent(validateFlowDeploymentPanel);            
        } else {
            deployReleasePackage();
            close();
        }
    }

    protected Component buildValidatePackageDeploymentAction() {
        if (validateFlowDeploymentPanel == null) {
            String introText = "Validate deployment actions";
            validateFlowDeploymentPanel = new ValidateFlowDeploymentPanel(
                    context, introText, getFlowsFromReleasePackages(selectPackagePanel.getSelectedPackages()),
                    parentPanel.getAgent().getId());
        }
        return validateFlowDeploymentPanel;
    }
    
    protected void deployReleasePackage() {
        BeanItemContainer<DeploymentLine> container = validateFlowDeploymentPanel.getContainer();
        for (int i=0; i<container.size();i++) {
            DeploymentLine line = container.getIdByIndex(i);
            Flow flow = configurationService.findFlow(line.getNewFlowId());
            AgentDeploy existingDeployment = operationsService.findAgentDeployment(line.getExistingDeploymentId());                
            deployFlow(flow, line.newDeployName, line.upgrade, existingDeployment);                
        }   
        deployResourceSettings(selectPackagePanel.getSelectedPackages());
    }
    
    protected void deployResourceSettings(List<ReleasePackage> releasePackages) {        
        for (ReleasePackage releasePackage : releasePackages) {
            releasePackage = configurationService.findReleasePackage(releasePackage.getId());
            processReleasePackageResources(releasePackage);
        }
    }
    
    protected void processReleasePackageResources(ReleasePackage releasePackage) {
        for (Rppv rppv : releasePackage.getProjectVersions()) {
            processProjectVersionResources(rppv);
        }
    }
    
    protected void processProjectVersionResources(Rppv rppv) {
        List<ResourceName> newResources = configurationService.findResourcesInProject(rppv.getProjectVersionId());
        Map<String, List<AgentResourceSetting>> agentResourceSettingsMap = buildAgentResourceSettingsMap(newResources);        
        for (ResourceName newResource : newResources) {
            List<AgentResourceSetting> agentResourceSettings = agentResourceSettingsMap.get(newResource.getRowId());
            for (AgentResourceSetting agentResourceSetting : agentResourceSettings) {
                agentResourceSetting.setResourceId(newResource.getId());
                configurationService.save(agentResourceSetting);
            }
        }   
    }
    
    protected Map<String, List<AgentResourceSetting>> buildAgentResourceSettingsMap(List<ResourceName> newResources) {
        Map<String, List<AgentResourceSetting>> resourceSettingsMap = new HashMap<String, List<AgentResourceSetting>>();
        for (ResourceName newResource : newResources) {
            resourceSettingsMap.put(newResource.getRowId(), 
                    operationsService.findMostRecentDeployedResourceSettings(parentPanel.getAgent().getId(), newResource.getId()));
        }
        return resourceSettingsMap;
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

    protected void deployFlow(Flow flow, String deployName, boolean upgrade, 
            AgentDeploy existingDeployment) {
        
        AgentDeploy newDeploy = new AgentDeploy();
        newDeploy.setAgentId(parentPanel.getAgent().getId());
        newDeploy.setName(deployName);
        newDeploy.setFlowId(flow.getId());
        List<AgentFlowDeployParm> newDeployParams = newDeploy.getAgentDeploymentParms();
        //initialize from the flow.  If upgrading replace with agent values
        for (FlowParameter flowParam : flow.getFlowParameters()) {
            AgentFlowDeployParm deployParam = new AgentFlowDeployParm();
            deployParam.setFlowId(flowParam.getFlowId());
            deployParam.setAgentDeploymentId(newDeploy.getId());
            deployParam.setName(flowParam.getName());
            deployParam.setValue(flowParam.getDefaultValue());
            newDeployParams.add(deployParam);
        }            
        if (upgrade) {
            List<AgentFlowDeployParm> existingDeployParams = existingDeployment.getAgentDeploymentParms();
            Map<String, String> existingDeployParamsMap = new HashMap<String, String>();
            for (AgentFlowDeployParm existingDeployParam : existingDeployParams) {
                existingDeployParamsMap.put(existingDeployParam.getName(), existingDeployParam.getValue());                
            }
            for (AgentFlowDeployParm newDeployParam : newDeployParams) {
                newDeployParam.setValue(existingDeployParamsMap.get(newDeployParam.getName()));
            }
            operationsService.delete(existingDeployment);
        }
        operationsService.save(newDeploy);
    }
    
    protected void deployFlows(Collection<FlowName> flowCollection) {
        for (FlowName flowName : flowCollection) {
            IConfigurationService configurationService = context.getConfigurationService();
            Flow flow = configurationService.findFlow(flowName.getId());
            deployFlow(flow, flow.getName(), false, null);
        }
        parentPanel.refresh();
        close();
    }

    protected String getName(String name) {
        for (Object deployment : parentPanel.getAgentDeploymentSummary()) {
            if (deployment instanceof AgentDeploy) {
                AgentDeploy agentDeployment = (AgentDeploy) deployment;
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
