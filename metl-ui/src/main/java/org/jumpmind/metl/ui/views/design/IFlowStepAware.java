package org.jumpmind.metl.ui.views.design;

import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;

public interface IFlowStepAware {
        
   public void makeAwareOf(FlowStep flowStep, Flow flow);

}
