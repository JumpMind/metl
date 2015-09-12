package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;

public interface IFlowStepAware {
        
   public void makeAwareOf(FlowStep flowStep, Flow flow);

}
