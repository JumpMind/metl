package org.jumpmind.metl.core.runtime.flow;

import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.persist.IConfigurationService;

public interface IFlowManipulator {

    public Flow manipulate(Flow flow, FlowStep flowStep, IConfigurationService configurationService);
    
}
