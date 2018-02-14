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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.metl.core.model.AgentDeploymentSummary;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IOperationsService;
import org.jumpmind.metl.ui.common.ApplicationContext;

import com.vaadin.annotations.StyleSheet;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.HeaderCell;
import com.vaadin.ui.Grid.HeaderRow;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.VerticalLayout;

@StyleSheet({ "mapping-diagram.css" })
public class ValidateFlowDeploymentPanel extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;
    
    IConfigurationService configurationService;
    
    IOperationsService operationsService;
    
    Grid grid;

    BeanItemContainer<DeploymentLine> container;
    
    List<FlowName> flows;
    
    String agentId;
   
    public ValidateFlowDeploymentPanel (ApplicationContext context, 
            String introText, List<FlowName> selectedFlows,
            String agentId) {
        this.context = context;
        this.configurationService = context.getConfigurationService();
        this.operationsService = context.getOperationsService();
        this.flows = selectedFlows;
        this.agentId = agentId;
        buildPanel(introText);
    }
    
    public BeanItemContainer<DeploymentLine> getContainer() {
        return container;
    }

    public void setContainer(BeanItemContainer<DeploymentLine> container) {
        this.container = container;
    }

    protected void buildPanel(String introText) {
        this.setSpacing(true);
        this.setSizeFull();
        buildGrid();
        buildGridHeader();
        addComponent(grid);
        setExpandRatio(grid, 1);
    }
    
    protected void buildGridHeader() {
        HeaderRow hdr1 = grid.prependHeaderRow();
        HeaderCell newDeployHdr = hdr1.join("newDeployName","newDeployVersion","newDeployType");
        newDeployHdr.setText("New Deployment");
        newDeployHdr.setStyleName("header-center");
        HeaderCell existingDeployHdr = hdr1.join("existingDeployName","existingDeployVersion","existingDeployType");
        existingDeployHdr.setText("Existing Deployment");
        existingDeployHdr.setStyleName("header-center");
        grid.getColumn("newDeployName").setHeaderCaption("Name");
        grid.getColumn("newDeployVersion").setHeaderCaption("Version");
        grid.getColumn("newDeployType").setHeaderCaption("Type");
        grid.getColumn("existingDeployName").setHeaderCaption("Name");
        grid.getColumn("existingDeployVersion").setHeaderCaption("Version");
        grid.getColumn("existingDeployType").setHeaderCaption("Type");
    }
    
    protected void buildGrid() {
        grid = new Grid();
        grid.setEditorEnabled(true);
        
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.SINGLE);
        grid.setColumns("projectName","newDeployName","newDeployVersion",
                "newDeployType","existingDeployName","existingDeployVersion",
                "existingDeployType","upgrade");        
        container = new BeanItemContainer<>(DeploymentLine.class);
        buildContainer();
        grid.setContainerDataSource(container);
        grid.sort("projectName", SortDirection.DESCENDING);        
    }
    
    protected void buildContainer() {
        Map<String,List<AgentDeploymentSummary>> deploymentsByFlow = buildExistingDeploymentsByFlow();
        for (FlowName flow:flows) {
            buildContainerFlows(deploymentsByFlow, flow);
        }
    }
    
    protected void buildContainerFlows(Map<String,List<AgentDeploymentSummary>> deploymentsByFlow,
            FlowName flow) {
        
        List<AgentDeploymentSummary> deploymentsForFlow = deploymentsByFlow.get(flow.getRowId());
        ProjectVersion projectVersion = configurationService.findProjectVersion(flow.getProjectVersionId());
        DeploymentLine deploymentLine = new DeploymentLine(projectVersion.getProject().getName(),flow.getId(),
                flow.getName(), projectVersion.getVersionLabel(),AgentDeploymentSummary.TYPE_FLOW,
                null, null, null,null,null,false);
        container.addItem(deploymentLine);
        int deploymentCount=0;
        while (deploymentsForFlow != null && deploymentCount < deploymentsForFlow.size()) {
            AgentDeploymentSummary deployment = deploymentsForFlow.get(deploymentCount);            
            if (deploymentCount == 0) {
                deploymentLine.setExistingDeploymentId(deployment.getId());
                deploymentLine.setExistingFlowId(deployment.getArtifactId());
                deploymentLine.setExistingDeployName(deployment.getName());
                deploymentLine.setExistingDeployType(deployment.getType());
                deploymentLine.setExistingDeployVersion(deployment.getProjectVersionLabel());
                deploymentLine.setUpgrade(true);
            } else {
                deploymentLine = new DeploymentLine(deployment.getProjectName(), flow.getId(), null, null, null,
                        deployment.getId(), deployment.getArtifactId(), deployment.getName(), deployment.getProjectVersionLabel(), deployment.getType(), true);
                container.addItem(deploymentLine);
            }
            deploymentCount++;
        }        
    }
    
    protected Map<String,List<AgentDeploymentSummary>> buildExistingDeploymentsByFlow() {
        Map<String,List<AgentDeploymentSummary>> deploymentsByFlowMap = new HashMap<String, List<AgentDeploymentSummary>>();
        List<AgentDeploymentSummary> deploymentList = operationsService.findAgentDeploymentSummary(agentId);
        for (AgentDeploymentSummary deployment : deploymentList) {
            List<AgentDeploymentSummary> deploymentsForFlow = deploymentsByFlowMap.get(deployment.getRowId());
            if (deploymentsForFlow == null) {
                deploymentsForFlow = new ArrayList<AgentDeploymentSummary>();
                deploymentsByFlowMap.put(deployment.getRowId(), deploymentsForFlow);
            }
            deploymentsForFlow.add(deployment);
        }
        return deploymentsByFlowMap;
    }
    
    public static class DeploymentLine {
 
        String projectName;
        String newFlowId;
        String newDeployName;
        String newDeployVersion;
        String newDeployType;
        String existingDeploymentId;
        String existingFlowId;
        String existingDeployName;
        String existingDeployVersion;
        String existingDeployType;
        boolean upgrade;

        public DeploymentLine(String projectName, String newFlowId, String newDeployName,
                String newDeployVersion, String newDeployType, String existingDeploymentId, String existingFlowId, 
                String existingDeployName, String existingDeployVersion, String existingDeployType, 
                boolean upgrade) {
            
            this.newFlowId = newFlowId;
            this.projectName = projectName;
            this.newDeployName = newDeployName;
            this.newDeployVersion = newDeployVersion;
            this.newDeployType = newDeployType;
            this.existingDeploymentId = existingDeploymentId;
            this.existingFlowId = existingFlowId;
            this.existingDeployName = existingDeployName;
            this.existingDeployVersion = existingDeployVersion;
            this.existingDeployType = existingDeployType;
            this.upgrade = upgrade;            
        }
    
        public String getProjectName() {
            return projectName;
        }
        public String getNewFlowId() {
            return newFlowId;
        }
        public void setNewFlowId(String newFlowId) {
            this.newFlowId = newFlowId;
        }
        public void setProjectName(String projectName) {
            this.projectName = projectName;
        }
        public String getNewDeployName() {
            return newDeployName;
        }
        public void setNewDeployName(String newDeployName) {
            this.newDeployName = newDeployName;
        }
        public String getNewDeployVersion() {
            return newDeployVersion;
        }
        public void setNewDeployVersion(String newDeployVersion) {
            this.newDeployVersion = newDeployVersion;
        }
        public String getNewDeployType() {
            return newDeployType;
        }
        public void setNewDeployType(String newDeployType) {
            this.newDeployType = newDeployType;
        }
        public String getExistingDeploymentId() {
            return existingDeploymentId;
        }
        public void setExistingDeploymentId(String existingDeploymentId) {
            this.existingDeploymentId = existingDeploymentId;
        }
        public String getExistingFlowId() {
            return existingFlowId;
        }
        public void setExistingFlowId(String existingFlowId) {
            this.existingFlowId = existingFlowId;
        }
        public String getExistingDeployName() {
            return existingDeployName;
        }
        public void setExistingDeployName(String existingDeployName) {
            this.existingDeployName = existingDeployName;
        }
        public String getExistingDeployVersion() {
            return existingDeployVersion;
        }
        public void setExistingDeployVersion(String existingDeployVersion) {
            this.existingDeployVersion = existingDeployVersion;
        }
        public String getExistingDeployType() {
            return existingDeployType;
        }
        public void setExistingDeployType(String existingDeployType) {
            this.existingDeployType = existingDeployType;
        }
        public boolean isUpgrade() {
            return upgrade;
        }
        public void setUpgrade(boolean upgrade) {
            this.upgrade = upgrade;
        }        
    }
}
