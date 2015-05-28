package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

public interface IFlowStepEditPanel extends IUiPanel {
    
    
   public void init(FlowStep flowStep, Flow flow, ApplicationContext context, PropertySheet propertySheet);

}
