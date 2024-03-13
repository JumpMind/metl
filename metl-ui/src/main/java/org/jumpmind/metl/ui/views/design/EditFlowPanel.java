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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import org.jumpmind.vaadin.ui.common.CustomSplitLayout;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dnd.DropEvent;
import com.vaadin.flow.component.dnd.DropTarget;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout.Orientation;
import com.vaadin.flow.dom.DomEvent;

import elemental.json.JsonObject;

@SuppressWarnings("serial")
public class EditFlowPanel extends HorizontalLayout implements IUiPanel, IFlowRunnable {

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    Flow flow;

    boolean readOnly;

    PropertySheet propertySheet;
    
    VerticalLayout propertySheetLayout;

    DesignNavigator designNavigator;

    EditFlowPalette componentPalette;
    
    FlowPaletteItem droppedItem;

    TabbedPanel tabs;

    Diagram diagram;

    Scroller flowPanel;

    VerticalLayout diagramLayout;

    Button runButton;

    Button copyButton;

    Button delButton;
    
    Button advancedEditButton;

    Button settingsButton;

    List<AbstractObject> selected = new ArrayList<AbstractObject>();

    IConfigurationService configurationService;
    
    VerticalLayout rightLayout;
    CustomSplitLayout vSplit;
    CustomSplitLayout hSplit;
    Boolean isVerticalView = null;

    double lastPos = 70;
    
    final static float MAX_PANEL_POSITION = 99;

    public EditFlowPanel(ApplicationContext context, String flowId, DesignNavigator designNavigator, TabbedPanel tabs) {
        this.configurationService = context.getConfigurationService();
        this.flow = configurationService.findFlow(flowId);
        this.readOnly = context.isReadOnly(configurationService.findProjectVersion(flow.getProjectVersionId()), Privilege.DESIGN);
        this.context = context;
        this.tabs = tabs;
        this.designNavigator = designNavigator;
        setSpacing(false);

        this.propertySheet = new PropertySheet(context, tabs, readOnly);
        this.propertySheet.setListener((components) -> {
            List<FlowStep> steps = new ArrayList<FlowStep>();
            for (Component c : components) {
                steps.add(EditFlowPanel.this.flow.findFlowStepWithComponentId(c.getId()));
            }
            refreshStepOnDiagram(steps);
        });
        propertySheetLayout = new VerticalLayout(propertySheet);
        propertySheetLayout.getStyle().set("padding", "0 16px");

        componentPalette = new EditFlowPalette(this, context, flow.getProjectVersionId());

        add(componentPalette);

        
        rightLayout = new VerticalLayout();
        rightLayout.setHeightFull();
        rightLayout.setWidth("0");
        rightLayout.setPadding(false);

        rightLayout.add(buildButtonBar());

        // Create two different layouts for the user to toggle between.
        vSplit = new CustomSplitLayout();
        vSplit.setOrientation(Orientation.VERTICAL);
        vSplit.setSizeFull();
        vSplit.setSplitterPosition(MAX_PANEL_POSITION);
        hSplit = new CustomSplitLayout();
        hSplit.setSizeFull();
        hSplit.setSplitterPosition(MAX_PANEL_POSITION);
         
        diagramLayout = new VerticalLayout();
        diagramLayout.setWidth("10000px");
        diagramLayout.setHeight("10000px");

        DropTarget<VerticalLayout> extension = DropTarget.create(diagramLayout);
        extension.addDropListener(new DropListener());
        extension.getElement().addEventListener("drop", this::onDrop)
            .addEventData("event.offsetX")
            .addEventData("event.offsetY");
        flowPanel = new Scroller();
        flowPanel.setSizeFull();
        flowPanel.setContent(diagramLayout);

        if (isVerticalView()) {
            vSplit.addToPrimary(flowPanel);
            vSplit.addToSecondary(propertySheetLayout);
            rightLayout.addAndExpand(vSplit);
        } else {
            hSplit.addToPrimary(flowPanel);
            hSplit.addToSecondary(propertySheetLayout);
            rightLayout.addAndExpand(hSplit);
        }

        add(rightLayout);
        expand(rightLayout);
    }

    protected HorizontalLayout buildButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        if (!readOnly) {
            runButton = buttonBar.addButton("Run", Icons.RUN);
            runButton.addClickListener((event)->runFlow());
        }

        settingsButton = buttonBar.addButton("Settings", VaadinIcon.COGS);
        settingsButton.addClickListener((event) -> new EditFlowSettingsDialog(context, flow, readOnly).open());        

        if (!readOnly) {
            Button selectAllButton = buttonBar.addButton("Select All", VaadinIcon.CROSSHAIRS);
            selectAllButton.addClickListener((event)->setSelectedAll());

            copyButton = buttonBar.addButton("Copy", VaadinIcon.COPY);
            copyButton.addClickListener((event)->copySelected());
            copyButton.setEnabled(false);

            delButton = buttonBar.addButton("Remove", VaadinIcon.TRASH);
            delButton.addClickListener((event)->deleteSelected());
            delButton.setEnabled(false);;

        }
        
        advancedEditButton = buttonBar.addButton("Advanced Edit", VaadinIcon.EDIT, e->openAdvancedEditor());
        advancedEditButton.setEnabled(false);

        Button exportButton = buttonBar.addButtonRight("Capture", VaadinIcon.CAMERA, (event)->export());
        exportButton.setId("exportButton");
        
        buttonBar.addButtonRight("Layout", VaadinIcon.SPLIT_H, (event)->toggleView());
        
        return buttonBar;
    }
    
    private void export() {
        // There is an issue with the html2canvas library not writing 
        // component anchors correctly when the scroll panel is scrolled down.
        // Reset scrolling first.
        //flowPanel.setScrollLeft(0);
        //flowPanel.setScrollTop(0);
        new ImagePreviewDialog(diagram).open();
    }
    
    private void toggleView() {
        if (isVerticalView()) {
            hSplit.addToPrimary(flowPanel);
            hSplit.addToSecondary(propertySheetLayout);
            rightLayout.replace(vSplit, hSplit);
            rightLayout.expand(hSplit);
            setVerticalView(false);
        } else {
            vSplit.addToPrimary(flowPanel);
            vSplit.addToSecondary(propertySheetLayout);
            rightLayout.replace(hSplit, vSplit);
            rightLayout.expand(vSplit);
            setVerticalView(true);
        }
    }

    protected Button createToolButton(String name, VaadinIcon icon) {
        Button button = new Button(name, new Icon(icon));
        button.addThemeVariants(ButtonVariant.LUMO_SMALL);
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
    	// Clone components
        Map<AbstractObject,AbstractObject> oldNew = new HashMap<AbstractObject,AbstractObject>(selected.size());
        for (AbstractObject s : selected) {
            if (s instanceof FlowStep) {
                FlowStep copy = configurationService.copy((FlowStep) s);
                copy.setX(copy.getX() + 20);
                copy.setY(copy.getY() + 20);
                copy.setName(copy.getName() + " Copy");

                flow.getFlowSteps().add(copy);
                configurationService.save(copy);
                oldNew.put(s,copy);
            }
        }
        
        // Clone links between selected components
        List<FlowStepLink> links = flow.getFlowStepLinks();
        List<FlowStepLink> newLinks = new ArrayList<FlowStepLink>();
        for (AbstractObject o : oldNew.keySet()) {
        	FlowStep old = (FlowStep) o;
        	for (FlowStepLink link : links) {
        		if (link.getSourceStepId().equals(old.getId())) {
        			FlowStep oldLinkTarget = (FlowStep) selected.stream()
        					.filter(l->l.getId().equals(link.getTargetStepId())).findFirst().orElse(null);
        			if (oldLinkTarget != null) {
	        			FlowStepLink newLink = new FlowStepLink(oldNew.get(o).getId(), oldNew.get(oldLinkTarget).getId());
	        			newLinks.add(newLink);
        			}
        		}
        	}
        }
		flow.getFlowStepLinks().addAll(newLinks);
		configurationService.save(flow);
        setSelectedNodes(new ArrayList<AbstractObject>(oldNew.values()));
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

    public void redrawFlow() {
        if (diagram != null) {
            diagramLayout.remove(diagram);
        }

        diagram = new Diagram(this);
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
        diagram.setNodes(getNodes());

        diagramLayout.add(diagram);
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
            tabs.addCloseableTab(deployment.getId(), "Call " + flow.getName(), new Icon(Icons.RUN), panel);
            tabs.setSelectedTab(panel);
        } else {
            String executionId = agentManager.getAgentRuntime(myDesignAgent.getId()).scheduleNow(context.getUser().getLoginId(), deployment,
                    flow.toFlowParametersAsString());
            if (executionId != null) {
                ExecutionRunPanel logPanel = new ExecutionRunPanel(executionId, context, tabs,
                        this);
                tabs.addCloseableTab(executionId, "Run " + flow.getName(), new Icon(Icons.LOG), logPanel);
                tabs.setSelectedTab(logPanel);
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
                        tabs.addCloseableTab(flowStep.getId(), flowStep.getName(), new Icon(Icons.COMPONENT),
                                (AbstractComponentEditPanel) panel);
                        tabs.setSelectedTab((AbstractComponentEditPanel) panel);
                    }
                }
       
            }
        }
    }
    
    protected void setPropertiesMinimized(boolean minimize) {
        CustomSplitLayout activePanel = isVerticalView() ? vSplit : hSplit;
        if (minimize 
                && vSplit.getSplitterPosition() != MAX_PANEL_POSITION 
                && hSplit.getSplitterPosition() != MAX_PANEL_POSITION) {
            lastPos = activePanel.getSplitterPosition();
            vSplit.setSplitterPosition(MAX_PANEL_POSITION);
            hSplit.setSplitterPosition(MAX_PANEL_POSITION);
        } else if (!minimize && activePanel.getSplitterPosition() == MAX_PANEL_POSITION) {
            vSplit.setSplitterPosition(lastPos);    
            hSplit.setSplitterPosition(lastPos);            
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

    public void nodeSelectedEvent(NodeSelectedEvent event) {
        setSelectedNodeIds(event.getNodeIds());
    }

    public void nodeDoubleClickedEvent(NodeDoubleClickedEvent event) {
        openAdvancedEditor();
    }

    public void nodeMovedEvent(NodeMovedEvent event) {
        Node node = event.getNode();
        FlowStep flowStep = flow.findFlowStepWithId(node.getId());
        if (flowStep != null) {
            flowStep.setX(node.getX());
            flowStep.setY(node.getY());
        }
        flow.calculateApproximateOrder();
        configurationService.save(flowStep);
    }

    public void linkEvent(LinkEvent event) {
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
                    targetComp.setInputModelId(sourceComp.getOutputModelId());
                } else if (sourceDefn.isInputOutputModelsMatch() && sourceComp.getInputModel() != null) {
                    targetComp.setInputModel(sourceComp.getInputModel());
                    targetComp.setInputModelId(sourceComp.getInputModelId());
                }
                
                if (targetDefn.isInputOutputModelsMatch()) {
                    targetComp.setOutputModel(targetComp.getInputModel());
                    targetComp.setOutputModelId(targetComp.getInputModelId());
                }
            }

            if (sourceComp.getOutputModel() == null) {
                if (targetComp.getInputModel() != null) {
                    sourceComp.setOutputModel(targetComp.getInputModel());
                    sourceComp.setOutputModelId(targetComp.getInputModelId());
                }
                
                if (sourceDefn.isInputOutputModelsMatch()) {
                    sourceComp.setInputModel(sourceComp.getOutputModel());
                    sourceComp.setInputModelId(sourceComp.getOutputModelId());
                }
            }

            if (sourceComp.getInputModel() == null && sourceDefn.isInputOutputModelsMatch()) {
                if (targetComp.getInputModel() != null) {
                    sourceComp.setInputModel(targetComp.getInputModel());
                    sourceComp.setInputModelId(targetComp.getInputModelId());
                    sourceComp.setOutputModel(targetComp.getInputModel());
                    sourceComp.setOutputModelId(targetComp.getInputModelId());
                }
            }

            configurationService.save(flow);
            propertySheet.setSource(propertySheet.getValue());
        } else {
            FlowStepLink link = flow.removeFlowStepLink(event.getSourceNodeId(), event.getTargetNodeId());
            if (link != null) {
                if (configurationService.delete(link)) {
                    redrawFlow();
                }
            }
        }
    }

    public void linkSelectedEvent(LinkSelectedEvent event) {
        selected = new ArrayList<AbstractObject>(1);
        selected.add(flow.findFlowStepLink(event.getSourceNodeId(), event.getTargetNodeId()));
        setSelectedLinks(selected);
    }
    
    private void onDrop(DomEvent event) {
        if (droppedItem != null) {
            JsonObject eventData = event.getEventData();
            double x = eventData.getNumber("event.offsetX");
            double y = eventData.getNumber("event.offsetY");
            if (droppedItem.isShared()) {
                Component component = new Component();
                component.setId(droppedItem.getComponentId());
                configurationService.refresh(component, true);
                addComponent(droppedItem.getText(), (int) x, (int) y, component);
            } else {
                Component component = new Component();
                component.setType(droppedItem.getComponentType());
                component.setShared(false);
                addComponent(droppedItem.getText(), (int) x, (int) y, component);
            }
            droppedItem = null;
        }
    }
    
    class DropListener implements ComponentEventListener<DropEvent<VerticalLayout>> {

        @Override
        public void onComponentEvent(DropEvent<VerticalLayout> event) {
            Object object = event.getDragSourceComponent().orElse(null);
            if (object instanceof FlowPaletteItem && !readOnly) {
                droppedItem = (FlowPaletteItem) object;
            }
        }
    }

}
