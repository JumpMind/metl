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
import org.jumpmind.metl.core.model.AgentDeploy;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowName;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.FlowStepLink;
import org.jumpmind.metl.core.model.Folder;
import org.jumpmind.metl.core.model.FolderType;
import org.jumpmind.metl.core.model.Privilege;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.model.UserSetting;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.MisconfiguredException;
import org.jumpmind.metl.core.runtime.component.AbstractComponentRuntime;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.IFlowRunnable;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.common.UiUtils;
import org.jumpmind.metl.ui.definition.XMLComponentUI;
import org.jumpmind.metl.ui.diagram.Diagram;
import org.jumpmind.metl.ui.diagram.LinkEvent;
import org.jumpmind.metl.ui.diagram.LinkSelectedEvent;
import org.jumpmind.metl.ui.diagram.Node;
import org.jumpmind.metl.ui.diagram.NodeDoubleClickedEvent;
import org.jumpmind.metl.ui.diagram.NodeMovedEvent;
import org.jumpmind.metl.ui.diagram.NodeSelectedEvent;
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
import com.vaadin.ui.AbstractSplitPanel;
import com.vaadin.ui.Button;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.WrapperTargetDetails;
import com.vaadin.ui.DragAndDropWrapper.WrapperTransferable;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
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
    
    Button advancedEditButton;

    Button settingsButton;

    List<AbstractObject> selected = new ArrayList<AbstractObject>();

    IConfigurationService configurationService;
    
    VerticalLayout rightLayout;
    AbstractSplitPanel vSplit;
    AbstractSplitPanel hSplit;
    Boolean isVerticalView = null;

    float lastPos = 70;
    
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
        propertySheet.setCaption("Property Sheet");

        componentPalette = new EditFlowPalette(this, context, flow.getProjectVersionId());

        addComponent(componentPalette);

        
        rightLayout = new VerticalLayout();
        rightLayout.setSizeFull();

        rightLayout.addComponent(buildButtonBar());

        // Create two different layouts for the user to toggle between.
        vSplit = new VerticalSplitPanel();
        vSplit.setSizeFull();
        vSplit.setSplitPosition(MAX_PANEL_POSITION, Unit.PERCENTAGE);
        hSplit = new HorizontalSplitPanel();
        hSplit.setSizeFull();
        hSplit.setSplitPosition(MAX_PANEL_POSITION, Unit.PERCENTAGE);
         
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

        if (isVerticalView()) {
            vSplit.addComponent(flowPanel);
            vSplit.addComponent(propertySheet);
            rightLayout.addComponent(vSplit);
            rightLayout.setExpandRatio(vSplit, 1);
        } else {
            hSplit.addComponent(flowPanel);
            hSplit.addComponent(propertySheet);
            rightLayout.addComponent(hSplit);
            rightLayout.setExpandRatio(hSplit, 1);
        }

        addComponent(rightLayout);
        setExpandRatio(rightLayout, 1);

        redrawFlow();
    }

    protected HorizontalLayout buildButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            runButton = buttonBar.addButton("Run", Icons.RUN);
            runButton.addClickListener((event)->runFlow());
        }

        settingsButton = buttonBar.addButton("Settings", FontAwesome.GEARS);
        settingsButton.addClickListener((event) -> new EditFlowSettingsDialog(context, flow, readOnly).showAtSize(.75));        

        if (!readOnly) {
            Button selectAllButton = buttonBar.addButton("Select All", FontAwesome.CROSSHAIRS);
            selectAllButton.addClickListener((event)->setSelectedAll());

            copyButton = buttonBar.addButton("Copy", FontAwesome.COPY);
            copyButton.addClickListener((event)->copySelected());
            copyButton.setEnabled(false);

            delButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
            delButton.addClickListener((event)->deleteSelected());
            delButton.setEnabled(false);;

        }
        
        advancedEditButton = buttonBar.addButton("Advanced Edit", FontAwesome.EDIT, e->openAdvancedEditor());
        advancedEditButton.setEnabled(false);

        Button exportButton = buttonBar.addButtonRight("Capture", FontAwesome.CAMERA, (event)->export());
        exportButton.setId("exportButton");
        
        buttonBar.addButtonRight("Layout", FontAwesome.COLUMNS, (event)->toggleView());
        
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
    
    private void toggleView() {
        if (isVerticalView()) {
            hSplit.addComponent(flowPanel);
            hSplit.addComponent(propertySheet);
            rightLayout.replaceComponent(vSplit, hSplit);
            rightLayout.setExpandRatio(hSplit, 1);
            setVerticalView(false);
        } else {
            vSplit.addComponent(flowPanel);
            vSplit.addComponent(propertySheet);
            rightLayout.replaceComponent(hSplit, vSplit);
            rightLayout.setExpandRatio(vSplit, 1);
            setVerticalView(true);
        }
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
                throw new MisconfiguredException("Could not find the component definition for a component of type '%s' and project version %s", type, flow.getProjectVersionId());
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
        
        final String AGENT_NAME = String.format("<%s on %s>", context.getUser().getLoginId(), AppUtils.getHostName());
        IAgentManager agentManager = context.getAgentManager();
        Set<Agent> agents = agentManager.getAvailableAgents();
        Agent myDesignAgent = null;
        for (Agent agent : agents) {
            if (agent.getFolder() != null && Agent.DESIGN_FOLDER_NAME.equals(agent.getFolder().getName()) && agent.getName().equals(AGENT_NAME)) {
                myDesignAgent = agent;
                break;
            }
        }

        if (myDesignAgent == null) {
            Folder folder = configurationService.findFirstFolderWithName(Agent.DESIGN_FOLDER_NAME, FolderType.AGENT);
            if (folder == null) {
                folder = new Folder();
                folder.setType(FolderType.AGENT.name());
                folder.setName(Agent.DESIGN_FOLDER_NAME);
                configurationService.save(folder);
            }

            myDesignAgent = new Agent();
            myDesignAgent.setName(AGENT_NAME);
            myDesignAgent.setFolder(folder);
            configurationService.save(myDesignAgent);
            agentManager.refresh(myDesignAgent);
        }

        AgentDeploy deployment = myDesignAgent.getAgentDeploymentFor(flow);
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
    
    protected boolean hasAdvancedEditor(List<AbstractObject> selected) {
        if (selected.size() == 1) {
        Iterator<AbstractObject> iter = selected.iterator();
        while (iter.hasNext()) {
            AbstractObject s = iter.next();
            if (s instanceof FlowStep) {
                FlowStep flowStep = (FlowStep) s;
                if (flowStep != null) {
                    Component component = flowStep.getComponent();
                    String type = component.getType();
                    XMLComponentUI definition = context.getUiFactory().getUiDefinition(component.getProjectVersionId(), type);
                    return definition != null && definition.getClassName() != null;
                }
            }
        }
        }
        return false;
    }
    
    public void openAdvancedEditor() {
        Iterator<AbstractObject> iter = selected.iterator();
        while (iter.hasNext()) {
            AbstractObject s = iter.next();
            if (s instanceof FlowStep) {
                FlowStep flowStep = (FlowStep)s;
                if (flowStep != null) {
                    Component component = flowStep.getComponent();
                    String type = component.getType();
                    IComponentEditPanel panel = context.getUiFactory().createUiPanel(component.getProjectVersionId(), type);
                    if (panel != null) {
                        if (panel instanceof IFlowStepAware) {
                            Flow flow = context.getConfigurationService().findFlow(flowStep.getFlowId());
                            ((IFlowStepAware) panel).makeAwareOf(flowStep, flow);
                        }
                        panel.init(readOnly, flowStep.getComponent(), context, propertySheet);
                        tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), Icons.COMPONENT, panel);
                    }
                }
       
            }
        }
    }
    
    protected void setPropertiesMinimized(boolean minimize) {
        AbstractSplitPanel activePanel = isVerticalView() ? vSplit : hSplit;
        if (minimize 
                && vSplit.getSplitPosition() != MAX_PANEL_POSITION 
                && hSplit.getSplitPosition() != MAX_PANEL_POSITION) {
            lastPos = activePanel.getSplitPosition();
            vSplit.setSplitPosition(MAX_PANEL_POSITION, Unit.PERCENTAGE);
            hSplit.setSplitPosition(MAX_PANEL_POSITION, Unit.PERCENTAGE);
        } else if (!minimize && activePanel.getSplitPosition() == MAX_PANEL_POSITION) {
            vSplit.setSplitPosition(lastPos, Unit.PERCENTAGE);    
            hSplit.setSplitPosition(lastPos, Unit.PERCENTAGE);            
        }
    }
    
    protected void setSelectedAll() {
        ArrayList<AbstractObject> selected = new ArrayList<>();
        for (FlowStep step : flow.getFlowSteps()) {
            selected.add(step);
        }
        setSelectedNodes(selected);
        redrawFlow();
    }
    
    protected void setSelectedNodes(List<AbstractObject> nodes) {
        setPropertiesMinimized(nodes.size()==0);
        
        if (nodes.size() == 0) {
            copyButton.setEnabled(false); 
            delButton.setEnabled(false);
        } else if (!readOnly) {
            delButton.setEnabled(true);
            copyButton.setEnabled(true);
        }
        selected = nodes;
        propertySheet.setSource(selected);
        advancedEditButton.setEnabled(hasAdvancedEditor(nodes));
    }
    
    protected void setSelectedLinks(List<AbstractObject> links) {
        if (links.size() > 0) {
            delButton.setEnabled(true);
        }
    }
    
    protected void setSelectedNodeIds(List<String> nodeIds) {
        ArrayList<AbstractObject> selected = new ArrayList<AbstractObject>(nodeIds.size());
        for (String id : nodeIds) {
            FlowStep flowStep = flow.findFlowStepWithId(id);
            selected.add(flowStep);
        }
        setSelectedNodes(selected);
    }

    protected void setSelectedFlowStep(FlowStep flowStep) {
        ArrayList<AbstractObject> selected = new ArrayList<AbstractObject>(1);
        if (flowStep != null) {
            selected.add(flowStep);
        }
        setSelectedNodes(selected);
    }

    protected void setSelectedEmpty() {
        ArrayList<AbstractObject> selected = new ArrayList<>(0);
        setSelectedNodes(selected);
        redrawFlow();
    }
    
    protected boolean isVerticalView() {
        if (isVerticalView != null) {
            return isVerticalView;
        } else {
            return Boolean.TRUE.toString().equals(context.getUser().findSetting(UserSetting.SETTING_FLOW_PANEL_VIEW_VERTICAL)
                    .getValue(Boolean.TRUE.toString()));
        }
    }
    
    protected void setVerticalView(Boolean isVerticalView) {
        this.isVerticalView = isVerticalView;
        Setting setting = context.getUser().findSetting(UserSetting.SETTING_FLOW_PANEL_VIEW_VERTICAL);
        setting.setValue(isVerticalView.toString());
        context.getConfigurationService().save(setting);
    }

    class DiagramChangedListener implements Listener {

        @Override
        public void componentEvent(Event e) {
            if (e instanceof NodeSelectedEvent) {
                NodeSelectedEvent event = (NodeSelectedEvent) e;                
                setSelectedNodeIds(event.getNodeIds());
            } else if (e instanceof NodeDoubleClickedEvent) {
                openAdvancedEditor();
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

                    configurationService.save(flow);
                } else {
                    FlowStepLink link = flow.removeFlowStepLink(event.getSourceNodeId(), event.getTargetNodeId());
                    if (link != null) {
                        if (configurationService.delete(link)) {
                            redrawFlow();
                        }
                    }
                }
            } else if (e instanceof LinkSelectedEvent) {
                LinkSelectedEvent event = (LinkSelectedEvent) e;
                selected = new ArrayList<AbstractObject>(1);
                selected.add(flow.findFlowStepLink(event.getSourceNodeId(), event.getTargetNodeId()));
                setSelectedLinks(selected);
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
