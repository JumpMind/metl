package org.jumpmind.metl.ui.views.deploy;

import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;

import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class ValidateReleasePackageDeploymentPanel extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;
    
    IConfigurationService configService;
    
    public ValidateReleasePackageDeploymentPanel (ApplicationContext context, 
            String introText) {
        this.context = context;
        buildPanel(introText);
    }

    protected void buildPanel(String introText) {
        this.setSpacing(true);
        this.setSizeFull();
        this.addComponent(new Label(introText));
    }
}
