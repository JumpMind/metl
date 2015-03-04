package org.jumpmind.symmetric.is.ui.views;

import java.util.List;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.model.ComponentFlowNodeLink;
import org.jumpmind.symmetric.is.core.model.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.model.ComponentVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.diagram.ConnectionEvent;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.diagram.Node;
import org.jumpmind.symmetric.is.ui.diagram.NodeMovedEvent;
import org.jumpmind.symmetric.is.ui.diagram.NodeSelectedEvent;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.ui.HorizontalLayout;

public class DesignFlowLayout extends HorizontalLayout implements IUiPanel {

    private static final long serialVersionUID = 1L;

    IConfigurationService configurationService;

    ComponentFlowVersion componentFlowVersion;

    DesignComponentPalette designComponentPalette;

    DesignPropertySheet designPropertySheet;

    DesignNavigator designNavigator;
    
    Diagram diagram;

    public DesignFlowLayout(IConfigurationService configurationService,
            ComponentFlowVersion componentFlowVersion,
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
        List<ComponentFlowNode> nodes = componentFlowVersion.getComponentFlowNodes();
        for (ComponentFlowNode componentFlowNode : nodes) {
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

        ComponentFlowNode componentFlowNode = new ComponentFlowNode(componentVersion);
        componentFlowNode.setComponentFlowVersionId(componentFlowVersion.getId());
        componentFlowVersion.getComponentFlowNodes().add(componentFlowNode);

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

        List<ComponentFlowNodeLink> links = componentFlowVersion.getComponentFlowNodeLinks();

        List<ComponentFlowNode> flowNodes = componentFlowVersion.getComponentFlowNodes();
        for (ComponentFlowNode flowNode : flowNodes) {
            Node node = new Node();
            String name = flowNode.getComponentVersion().getComponent().getName();
            String type = flowNode.getComponentVersion().getComponent().getType();
            node.setText(name + "<br><i>" + type + "</i>");
            node.setId(flowNode.getId());
            node.setX(flowNode.getX());
            node.setY(flowNode.getY());
            diagram.addNode(node);

            for (ComponentFlowNodeLink link : links) {
                if (link.getSourceNodeId().equals(node.getId())) {
                    node.getTargetNodeIds().add(link.getTargetNodeId());
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
                ComponentFlowNode flowNode = componentFlowVersion.findComponentFlowNodeWithId(node
                        .getId());
                designPropertySheet.valueChange(flowNode.getComponentVersion());

            } else if (e instanceof NodeMovedEvent) {
                NodeMovedEvent event = (NodeMovedEvent) e;
                Node node = event.getNode();
                ComponentFlowNode flowNode = componentFlowVersion.findComponentFlowNodeWithId(node
                        .getId());
                if (flowNode != null) {
                    flowNode.setX(node.getX());
                    flowNode.setY(node.getY());
                }
                configurationService.save(componentFlowVersion);

            } else if (e instanceof ConnectionEvent) {
                ConnectionEvent event = (ConnectionEvent) e;
                if (!event.isRemoved()) {
                    componentFlowVersion.getComponentFlowNodeLinks().add(
                            new ComponentFlowNodeLink(event.getSourceNodeId(), event
                                    .getTargetNodeId()));
                    configurationService.save(componentFlowVersion);
                } else {
                    ComponentFlowNodeLink link = componentFlowVersion.removeComponentFlowNodeLink(
                            event.getSourceNodeId(), event.getTargetNodeId());
                    if (link != null) {
                        configurationService.delete(link);
                    }

                }
            }
        }
    }

}
