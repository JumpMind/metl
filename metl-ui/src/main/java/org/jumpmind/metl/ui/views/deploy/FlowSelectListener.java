package org.jumpmind.metl.ui.views.deploy;

import java.util.Collection;

import org.jumpmind.metl.core.model.FlowName;

public interface FlowSelectListener {

    public void selected(Collection<FlowName> flowCollection);

}
