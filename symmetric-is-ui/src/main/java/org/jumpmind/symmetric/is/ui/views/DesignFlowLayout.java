package org.jumpmind.symmetric.is.ui.views;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.model.FlowVersion;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.diagram.ResourceEvent;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.diagram.Node;
import org.jumpmind.symmetric.is.ui.diagram.NodeMovedEvent;
import org.jumpmind.symmetric.is.ui.diagram.NodeSelectedEvent;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.ui.HorizontalLayout;

public class DesignFlowLayout extends HorizontalLayout implements IUiPanel {

    private static final long serialVersionUID = 1L;

    IConfigurationService configurationService;

    FlowVersion componentFlowVersion;

    DesignComponentPalette designComponentPalette;

    DesignPropertySheet designPropertySheet;

    DesignNavigator designNavigator;
    
    Diagram diagram;

    public DesignFlowLayout(IConfigurationService configurationService,
            FlowVersion componentFlowVersion,
            DesignComponentPalette designComponentPalette, DesignPropertySheet designPropertySheet,
            DesignNavigator designNavigator) {
        this.configurationService = configurationService;
        this.componentFlowVersion = componentFlowVersion;
        this.designComponentPalette = designComponentPalette;
        this.designPropertySheet = designPropertySheet;
        this.designNavigator = designNavigator;   
        redrawFlow();
    }

    @Override
    public boolean closing() {
        this.designComponentPalette.setVisible(false);
        return true;
    }

    @Override
    public void showing() {
        this.designComponentPalette.setVisible(true);
        this.designComponentPalette.setCurrentlySelectedDesignFlowLayout(this);
    }

    protected int countComponentsOfType(String type) {
        int count = 0;
        List<FlowStep> nodes = componentFlowVersion.getFlowSteps();
        for (FlowStep componentFlowNode : nodes) {
            if (componentFlowNode.getComponentVersion().getComponent().getType().equals(type)) {
                count++;
            }
        }
        return count;
    }

    protected void addComponent(Component component) {
        ComponentVersion componentVersion = new ComponentVersion(component, null, null, null, null,
                null);
        componentVersion.setVersionName("version 1.0");

        component.setName(component.getType() + " "
                + (countComponentsOfType(component.getType()) + 1));

        FlowStep componentFlowNode = new FlowStep(componentVersion);
        componentFlowNode.setFlowVersionId(componentFlowVersion.getId());
        componentFlowVersion.getFlowSteps().add(componentFlowNode);

        configurationService.save(componentFlowNode);

        redrawFlow();

        designPropertySheet.valueChange(componentVersion);

       
    }
    
    protected void redrawFlow() {
        if (diagram != null) {
            removeComponent(diagram);
        }

        diagram = new Diagram();
        diagram.addListener(new DiagramChangedListener());
        addComponent(diagram);

        List<FlowStepLink> links = componentFlowVersion.getFlowStepLinks();

        List<FlowStep> flowNodes = componentFlowVersion.getFlowSteps();
        for (FlowStep flowNode : flowNodes) {
            Node node = new Node();
            String name = flowNode.getComponentVersion().getComponent().getName();
            String type = flowNode.getComponentVersion().getComponent().getType();
            node.setText(name + "<br><i>" + type + "</i>");
            node.setId(flowNode.getId());
            node.setX(flowNode.getX());
            node.setY(flowNode.getY());
            diagram.addNode(node);

            for (FlowStepLink link : links) {
                if (link.getSourceStepId().equals(node.getId())) {
                    node.getTargetNodeIds().add(link.getTargetStepId());
                }
            }

        }

    }
    
    class DiagramChangedListener implements Listener {
        private static final long serialVersionUID = 1L;

        @Override
        public void componentEvent(Event e) {
            if (e instanceof NodeSelectedEvent) {
                NodeSelectedEvent event = (NodeSelectedEvent) e;
                Node node = event.getNode();
                FlowStep flowNode = componentFlowVersion.findFlowStepWithId(node
                        .getId());
                designPropertySheet.valueChange(flowNode.getComponentVersion());

            } else if (e instanceof NodeMovedEvent) {
                NodeMovedEvent event = (NodeMovedEvent) e;
                Node node = event.getNode();
                FlowStep flowNode = componentFlowVersion.findFlowStepWithId(node
                        .getId());
                if (flowNode != null) {
                    flowNode.setX(node.getX());
                    flowNode.setY(node.getY());
                }
                configurationService.save(componentFlowVersion);

            } else if (e instanceof ResourceEvent) {
                ResourceEvent event = (ResourceEvent) e;
                if (!event.isRemoved()) {
                    componentFlowVersion.getFlowStepLinks().add(
                            new FlowStepLink(event.getSourceNodeId(), event
                                    .getTargetNodeId()));
                    configurationService.save(componentFlowVersion);
                } else {
                    FlowStepLink link = componentFlowVersion.removeFlowStepLink(
                            event.getSourceNodeId(), event.getTargetNodeId());
                    if (link != null) {
                        configurationService.delete(link);
                    }

                }
            }
        }
    }

}
