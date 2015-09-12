package org.jumpmind.metl.ui.views.design;

import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;

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
