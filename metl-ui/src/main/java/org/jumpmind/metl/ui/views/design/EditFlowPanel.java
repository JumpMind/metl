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
package org.jumpmind.metl.ui.views.design;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.model.AgentDeployment;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.metl.ui.diagram.Diagram;
import org.jumpmind.metl.ui.diagram.LinkEvent;
import org.jumpmind.metl.ui.diagram.LinkSelectedEvent;
import org.jumpmind.metl.ui.diagram.Node;
import org.jumpmind.metl.ui.diagram.NodeDoubleClickedEvent;
import org.jumpmind.metl.ui.diagram.NodeMovedEvent;
import org.jumpmind.metl.ui.diagram.NodeSelectedEvent;
import org.jumpmind.metl.ui.views.CallWebServicePanel;
import org.jumpmind.metl.ui.views.IFlowRunnable;
import org.jumpmind.metl.ui.views.manage.ExecutionRunPanel;
import org.jumpmind.util.AppUtils;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.WrapperTargetDetails;
import com.vaadin.ui.DragAndDropWrapper.WrapperTransferable;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditFlowPanel extends HorizontalLayout implements IUiPanel, IFlowRunnable {

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    Flow flow;

    boolean readOnly;

    PropertySheet propertySheet;

    DesignNavigator designNavigator;

    EditFlowPalette componentPalette;

    TabbedPanel tabs;

    Diagram diagram;

    Panel flowPanel;

    AbstractLayout diagramLayout;

    Button runButton;

    Button copyButton;

    Button delButton;

    Button parametersButton;
    
    VerticalSplitPanel splitPanel;

    List<AbstractObject> selected = new ArrayList<AbstractObject>();

    IConfigurationService configurationService;
    
    float lastPosition = 50;
    
    final static float MAX_PANEL_POSITION = 99;

    public EditFlowPanel(ApplicationContext context, String flowId, DesignNavigator designNavigator, TabbedPanel tabs) {
        this.configurationService = context.getConfigurationService();
        this.flow = configurationService.findFlow(flowId);
        this.readOnly = context.isReadOnly(configurationService.findProjectVersion(flow.getProjectVersionId()), Privilege.DESIGN);
        this.context = context;
        this.tabs = tabs;
        this.designNavigator = designNavigator;

        this.propertySheet = new PropertySheet(context, tabs, readOnly);
        this.propertySheet.setListener((components) -> {
            List<FlowStep> steps = new ArrayList<FlowStep>();
            for (Component c : components) {
                steps.add(EditFlowPanel.this.flow.findFlowStepWithComponentId(c.getId()));
            }
            refreshStepOnDiagram(steps);
        });
        this.propertySheet.setCaption("Property Sheet");

        this.componentPalette = new EditFlowPalette(this, context, flow.getProjectVersionId());

        addComponent(componentPalette);

        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setSizeFull();

        rightLayout.addComponent(buildButtonBar());

        splitPanel = new VerticalSplitPanel();
        splitPanel.setSizeFull();
        splitPanel.setSplitPosition(MAX_PANEL_POSITION, Unit.PERCENTAGE);

        diagramLayout = new VerticalLayout();
        diagramLayout.setWidth(10000, Unit.PIXELS);
        diagramLayout.setHeight(10000, Unit.PIXELS);

        DragAndDropWrapper wrapper = new DragAndDropWrapper(diagramLayout);
        wrapper.setSizeUndefined();
        wrapper.setDropHandler(new DropHandler());

        flowPanel = new Panel();
        flowPanel.setSizeFull();
        flowPanel.addStyleName(ValoTheme.PANEL_WELL);
        flowPanel.setContent(wrapper);

        splitPanel.addComponent(flowPanel);
        splitPanel.addComponent(propertySheet);

        rightLayout.addComponent(splitPanel);
        rightLayout.setExpandRatio(splitPanel, 1);

        addComponent(rightLayout);
        setExpandRatio(rightLayout, 1);

        redrawFlow();
    }

    protected HorizontalLayout buildButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            runButton = buttonBar.addButton("Run", Icons.RUN);
            runButton.addClickListener((event)->runFlow());
            
            Button selectAllButton = buttonBar.addButton("Select All", FontAwesome.CROSSHAIRS);
            selectAllButton.addClickListener((event)->setSelectedAll());

            copyButton = buttonBar.addButton("Copy", FontAwesome.COPY);
            copyButton.addClickListener((event)->copySelected());

            delButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
            delButton.addClickListener((event)->deleteSelected());
            delButton.setEnabled(false);

        }

        Button exportButton = buttonBar.addButtonRight("Capture", FontAwesome.CAMERA, (event)->export());
        exportButton.setId("exportButton");

        parametersButton = buttonBar.addButton("Settings", FontAwesome.GEARS);
        parametersButton.addClickListener((event) -> new EditFlowSettingsDialog(context, flow, readOnly).showAtSize(.75));

        return buttonBar;
    }
    
    private void export() {
        // There is an issue with the html2canvas library not writing 
        // component anchors correctly when the scroll panel is scrolled down.
        // Reset scrolling first.
        flowPanel.setScrollLeft(0);
        flowPanel.setScrollTop(0);
        new ImagePreviewDialog(diagram).showAtSize(.75);
    }

    protected Button createToolButton(String name, Resource icon) {
        Button button = new Button(name, icon);
        button.addStyleName(ValoTheme.BUTTON_SMALL);
        button.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        return button;
    }

    @Override
    public boolean closing() {
        FlowName flowName = new FlowName(flow);
        configurationService.save(flowName);
        return true;
    }

    @Override
    public void selected() {
        context.setCurrentFlow(new FlowName(flow));
        propertySheet.setSource(propertySheet.getValue());
    }

    public Flow getFlow() {
        return flow;
    }

    protected void refreshStepOnDiagram(List<FlowStep> steps) {
        List<String> ids = new ArrayList<String>(steps.size());
        for (FlowStep step : steps) {
            configurationService.refresh(step.getComponent(), true);
            ids.add(step.getId());
        }
        diagram.setNodes(getNodes());
        diagram.setSelectedNodeIds(ids);
    }

    @Override
    public void deselected() {
    }

    protected void copySelected() {
        List<AbstractObject> copies = new ArrayList<AbstractObject>(selected.size());
        for (AbstractObject s : selected) {
            if (s instanceof FlowStep) {
                FlowStep copy = configurationService.copy((FlowStep) s);
                copy.setX(copy.getX() + 20);
                copy.setY(copy.getY() + 20);
                copy.setName(copy.getName() + " Copy");

                flow.getFlowSteps().add(copy);
                configurationService.save(copy);
                copies.add(copy);
            }
        }
        
        setSelectedNodes(copies);
        
        redrawFlow();
    }

    protected void deleteSelected() {
        Iterator<AbstractObject> iter = selected.iterator();
        while (iter.hasNext()) {
            AbstractObject s = iter.next();
            if (s instanceof FlowStep) {
                FlowStep flowStep = (FlowStep) s;
                configurationService.delete(flow, flowStep);
                iter.remove();
            } else if (s instanceof FlowStepLink) {
                FlowStepLink link = (FlowStepLink) s;
                configurationService.delete(link);
                flow.removeFlowStepLink(link.getSourceStepId(), link.getTargetStepId());
                iter.remove();
            }
        }
        
        setSelectedEmpty();
        redrawFlow();
    }

    protected int countComponentsOfType(String type) {
        int count = 0;
        List<FlowStep> nodes = flow.getFlowSteps();
        for (FlowStep componentflowStep : nodes) {
            if (componentflowStep.getComponent().getType().equals(type)) {
                count++;
            }
        }
        return count;
    }

    protected void addComponent(String name, int x, int y, Component component) {
        component.setName(name + " " + (countComponentsOfType(component.getType()) + 1));
        component.setProjectVersionId(flow.getProjectVersionId());

        FlowStep flowStep = new FlowStep(component);
        flowStep.setX(x);
        flowStep.setY(y);
        flowStep.setFlowId(flow.getId());
        flow.getFlowSteps().add(flowStep);
        context.getConfigurationService().save(flowStep);

        setSelectedFlowStep(flowStep);

        redrawFlow();
        designNavigator.refresh();
    }

    protected void redrawFlow() {
        if (diagram != null) {
            diagramLayout.removeComponent(diagram);
        }

        diagram = new Diagram(readOnly);
        List<String> ids = new ArrayList<String>(selected.size());
        for (AbstractObject s : selected) {
            if (s instanceof FlowStep) {
                ids.add(s.getId());
            }
        }

        if (!readOnly) {
            delButton.setEnabled(ids.size() > 0);
            copyButton.setEnabled(ids.size() > 0);
        }

        diagram.setSelectedNodeIds(ids);
        diagram.setSizeFull();
        diagram.addListener(new DiagramChangedListener());
        diagram.setNodes(getNodes());

        diagramLayout.addComponent(diagram);
    }

    protected List<Node> getNodes() {
        List<FlowStep> flowSteps = flow.getFlowSteps();
        List<FlowStepLink> links = flow.getFlowStepLinks();
        List<Node> list = new ArrayList<Node>();
        for (FlowStep flowStep : flowSteps) {
            Node node = new Node();
            String name = flowStep.getComponent().getName();
            String type = flowStep.getComponent().getType();
            boolean enabled = flowStep.getComponent().getBoolean(AbstractComponentRuntime.ENABLED, true);
            String imageText = String.format(
                    "<img style=\"display: block; margin-left: auto; margin-right: auto\" src=\"data:image/png;base64,%s\"/>",
                    UiUtils.getBase64RepresentationOfImageForComponentType(flow.getProjectVersionId(), type, context));

            node.setText(imageText);
            node.setName(name);
            node.setEnabled(enabled);
            node.setId(flowStep.getId());
            node.setX(flowStep.getX());
            node.setY(flowStep.getY());

            XMLComponentDefinition definition = context.getDefinitionFactory().getComponentDefinition(flow.getProjectVersionId(), type);
            if (definition == null) {
                throw new MisconfiguredException("Could not find the component defintion for a component of type '%s'", type);
            }
            node.setInputLabel(definition.getInputMessageType().getLetter());
            node.setOutputLabel(definition.getOutputMessageType().getLetter());

            for (FlowStepLink link : links) {
                if (link.getSourceStepId().equals(node.getId())) {
                    node.getTargetNodeIds().add(link.getTargetStepId());
                }
            }

            list.add(node);

        }
        return list;
    }

    public void runFlow() {
        final String DESIGN_FOLDER_NAME = "<Design Time>";
        final String AGENT_NAME = String.format("<%s on %s>", context.getUser().getLoginId(), AppUtils.getHostName());
        IAgentManager agentManager = context.getAgentManager();
        Set<Agent> agents = agentManager.getAvailableAgents();
        Agent myDesignAgent = null;
        for (Agent agent : agents) {
            if (agent.getFolder() != null && DESIGN_FOLDER_NAME.equals(agent.getFolder().getName()) && agent.getName().equals(AGENT_NAME)) {
                myDesignAgent = agent;
                break;
            }
        }

        if (myDesignAgent == null) {
            Folder folder = configurationService.findFirstFolderWithName(DESIGN_FOLDER_NAME, FolderType.AGENT);
            if (folder == null) {
                folder = new Folder();
                folder.setType(FolderType.AGENT.name());
                folder.setName(DESIGN_FOLDER_NAME);
                configurationService.save(folder);
            }

            myDesignAgent = new Agent();
            myDesignAgent.setName(AGENT_NAME);
            myDesignAgent.setFolder(folder);
            configurationService.save(myDesignAgent);
            agentManager.refresh(myDesignAgent);
        }

        AgentDeployment deployment = myDesignAgent.getAgentDeploymentFor(flow);
        AgentRuntime runtime = agentManager.getAgentRuntime(myDesignAgent.getId());
        if (deployment != null) {
            runtime.undeploy(deployment);
        } 
        deployment = runtime.deploy(flow, null);

        if (flow.isWebService()) {
            CallWebServicePanel panel = new CallWebServicePanel(deployment, context, tabs);
            tabs.addCloseableTab(deployment.getId(), "Call " + flow.getName(), Icons.RUN, panel);
        } else {
            String executionId = agentManager.getAgentRuntime(myDesignAgent.getId()).scheduleNow(context.getUser().getLoginId(), deployment,
                    flow.toFlowParametersAsString());
            if (executionId != null) {
                ExecutionRunPanel logPanel = new ExecutionRunPanel(executionId, context, tabs,
                        this);
                tabs.addCloseableTab(executionId, "Run " + flow.getName(), Icons.LOG, logPanel);
                logPanel.onBackgroundUIRefresh(logPanel.onBackgroundDataRefresh());
            }
        }
    }
    
    protected void setPropertiesMinimized(boolean minimize) {
        float position = splitPanel.getSplitPosition();
        if (minimize && position != MAX_PANEL_POSITION) {
            lastPosition = position;
            splitPanel.setSplitPosition(MAX_PANEL_POSITION, Unit.PERCENTAGE);
        } else if (!minimize && position == MAX_PANEL_POSITION) {
            splitPanel.setSplitPosition(lastPosition, Unit.PERCENTAGE);
        }
    }
    
    protected void setSelectedAll() {
        setPropertiesMinimized(flow.getFlowSteps().size()==0);
        selected = new ArrayList<>();
        for (FlowStep step : flow.getFlowSteps()) {
            selected.add(step);
        }
        propertySheet.setSource(selected);
        redrawFlow();
    }
    
    protected void setSelectedNodes(List<AbstractObject> nodes) {
        setPropertiesMinimized(nodes.size()==0);
        selected = nodes;
        propertySheet.setSource(selected);
    }
    
    protected void setSelectedNodeIds(List<String> nodeIds) {
        setPropertiesMinimized(nodeIds.size()==0);
        selected = new ArrayList<AbstractObject>(nodeIds.size());
        for (String id : nodeIds) {
            FlowStep flowStep = flow.findFlowStepWithId(id);
            selected.add(flowStep);
        }
        propertySheet.setSource(selected);
    }
    
    protected void setSelectedFlowStep(FlowStep flowStep) {
        setPropertiesMinimized(flowStep==null);
        selected = new ArrayList<AbstractObject>(1);
        selected.add(flowStep);
        propertySheet.setSource(selected);
    }
    
    protected void setSelectedEmpty() {
        setPropertiesMinimized(true);
        propertySheet.setSource(null);
    }

    class DiagramChangedListener implements Listener {

        @Override
        public void componentEvent(Event e) {
            if (e instanceof NodeSelectedEvent) {
                NodeSelectedEvent event = (NodeSelectedEvent) e;
                
                setSelectedNodeIds(event.getNodeIds());
                
                
                if (!readOnly) {
                    delButton.setEnabled(true);
                    copyButton.setEnabled(true);
                }
            } else if (e instanceof NodeDoubleClickedEvent) {
                propertySheet.openAdvancedEditor();
            } else if (e instanceof NodeMovedEvent) {
                NodeMovedEvent event = (NodeMovedEvent) e;
                Node node = event.getNode();
                FlowStep flowStep = flow.findFlowStepWithId(node.getId());
                if (flowStep != null) {
                    flowStep.setX(node.getX());
                    flowStep.setY(node.getY());
                }
                flow.calculateApproximateOrder();
                configurationService.save(flowStep);
            } else if (e instanceof LinkEvent) {
                LinkEvent event = (LinkEvent) e;
                if (!event.isRemoved()) {
                    flow.getFlowStepLinks().add(new FlowStepLink(event.getSourceNodeId(), event.getTargetNodeId()));
                    Component sourceComp = flow.findFlowStepWithId(event.getSourceNodeId()).getComponent();
                    Component targetComp = flow.findFlowStepWithId(event.getTargetNodeId()).getComponent();
                    IDefinitionFactory factory = context.getDefinitionFactory();
                    XMLComponentDefinition sourceDefn = factory.getComponentDefinition(flow.getProjectVersionId(), sourceComp.getType());
                    XMLComponentDefinition targetDefn = factory.getComponentDefinition(flow.getProjectVersionId(), targetComp.getType());

                    if (targetComp.getInputModel() == null) {
                        if (sourceComp.getOutputModel() != null) {
                            targetComp.setInputModel(sourceComp.getOutputModel());
                        } else if (sourceDefn.isInputOutputModelsMatch() && sourceComp.getInputModel() != null) {
                            targetComp.setInputModel(sourceComp.getInputModel());
                        }
                        
                        if (targetDefn.isInputOutputModelsMatch()) {
                            targetComp.setOutputModel(targetComp.getInputModel());
                        }
                    }

                    if (sourceComp.getOutputModel() == null) {
                        if (targetComp.getInputModel() != null) {
                            sourceComp.setOutputModel(targetComp.getInputModel());
                        }
                        
                        if (sourceDefn.isInputOutputModelsMatch()) {
                            sourceComp.setInputModel(sourceComp.getOutputModel());
                        }
                    }

                    if (sourceComp.getInputModel() == null && sourceDefn.isInputOutputModelsMatch()) {
                        if (targetComp.getInputModel() != null) {
                            sourceComp.setInputModel(targetComp.getInputModel());
                            sourceComp.setOutputModel(targetComp.getInputModel());
                        }
                    }

                    if (!readOnly) {
                        delButton.setEnabled(false);
                        copyButton.setEnabled(false);
                    }

                    configurationService.save(flow);
                } else {
                    FlowStepLink link = flow.removeFlowStepLink(event.getSourceNodeId(), event.getTargetNodeId());
                    if (link != null) {
                        if (configurationService.delete(link)) {
                            redrawFlow();
                        }
                    }

                    if (!readOnly) {
                        delButton.setEnabled(false);
                        copyButton.setEnabled(false);
                    }
                }
            } else if (e instanceof LinkSelectedEvent) {
                LinkSelectedEvent event = (LinkSelectedEvent) e;
                selected = new ArrayList<AbstractObject>(1);
                selected.add(flow.findFlowStepLink(event.getSourceNodeId(), event.getTargetNodeId()));
                if (!readOnly) {
                    delButton.setEnabled(true);
                    copyButton.setEnabled(true);
                }
            }
        }

    }

    class DropHandler implements com.vaadin.event.dd.DropHandler {

        @Override
        public void drop(DragAndDropEvent event) {
            WrapperTransferable t = (WrapperTransferable) event.getTransferable();
            WrapperTargetDetails details = (WrapperTargetDetails) event.getTargetDetails();
            DragAndDropWrapper wrapper = (DragAndDropWrapper) t.getSourceComponent();
            Object object = wrapper.iterator().next();
            if (object instanceof FlowPaletteItem && !readOnly) {
                FlowPaletteItem flowPaletteItem = (FlowPaletteItem) object;
                if (flowPaletteItem.isShared()) {
                    Component component = new Component();
                    component.setId(flowPaletteItem.getComponentId());
                    configurationService.refresh(component, true);
                    addComponent(flowPaletteItem.getCaption(),
                            details.getMouseEvent().getClientX() - details.getAbsoluteLeft(),
                            details.getMouseEvent().getClientY() - details.getAbsoluteTop(),
                            component);
                } else {
                    Component component = new Component();
                    component.setType(flowPaletteItem.getComponentType());
                    component.setShared(false);
                    addComponent(flowPaletteItem.getCaption(),
                            details.getMouseEvent().getClientX() - details.getAbsoluteLeft(),
                            details.getMouseEvent().getClientY() - details.getAbsoluteTop(),
                            component);
                }
            }
        }

        @Override
        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }
    }

}
