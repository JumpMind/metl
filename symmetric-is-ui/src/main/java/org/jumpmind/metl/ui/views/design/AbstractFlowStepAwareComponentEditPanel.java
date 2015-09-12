package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;

public abstract class AbstractFlowStepAwareComponentEditPanel extends AbstractComponentEditPanel implements IFlowStepAware {

    private static final long serialVersionUID = 1L;
    
    protected Flow flow;
    
    protected FlowStep flowStep;
    
    @Override
    public void makeAwareOf(FlowStep flowStep, Flow flow) {
        this.flowStep = flowStep;
        this.flow = flow;
    }

    
    
}
