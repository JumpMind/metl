package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;

import com.vaadin.ui.HorizontalLayout;

public class DesignFlowLayout extends HorizontalLayout {

    private static final long serialVersionUID = 1L;

    IConfigurationService configurationService;
    
    ComponentFlowVersion flowVersion;
    
    public DesignFlowLayout(IConfigurationService configurationService, ComponentFlowVersion flowVersion) {
        this.configurationService = configurationService;
        this.flowVersion = flowVersion;
    }
}
