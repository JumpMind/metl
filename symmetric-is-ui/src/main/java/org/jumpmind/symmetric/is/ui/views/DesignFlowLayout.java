package org.jumpmind.symmetric.is.ui.views;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.Component;
import org.jumpmind.symmetric.is.core.config.ComponentFlowNode;
import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.config.ComponentVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.ui.HorizontalLayout;

public class DesignFlowLayout extends HorizontalLayout implements IUiPanel {

    private static final long serialVersionUID = 1L;

    IConfigurationService configurationService;

    ComponentFlowVersion componentFlowVersion;

    DesignComponentPalette designComponentPalette;

    DesignPropertySheet designPropertySheet;

    DesignNavigator designNavigator;

    public DesignFlowLayout(IConfigurationService configurationService,
            ComponentFlowVersion componentFlowVersion,
            DesignComponentPalette designComponentPalette, DesignPropertySheet designPropertySheet,
            DesignNavigator designNavigator) {
        this.configurationService = configurationService;
        this.componentFlowVersion = componentFlowVersion;
        this.designComponentPalette = designComponentPalette;
        this.designPropertySheet = designPropertySheet;
        this.designNavigator = designNavigator;                
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

        component.setName(component.getType() + " "
                + (countComponentsOfType(component.getType()) + 1));

        ComponentFlowNode componentFlowNode = new ComponentFlowNode(componentVersion);
        componentFlowNode.setComponentFlowVersionId(componentFlowVersion.getId());
        componentFlowVersion.getComponentFlowNodes().add(componentFlowNode);

        configurationService.save(componentFlowNode);

        // redrawFlow();

        designPropertySheet.valueChange(componentVersion);

       
    }
}
