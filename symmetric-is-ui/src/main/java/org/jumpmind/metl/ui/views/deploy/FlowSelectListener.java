package org.jumpmind.symmetric.is.ui.views.deploy;

import java.util.Collection;

import org.jumpmind.symmetric.is.core.model.FlowName;

public interface FlowSelectListener {

    public void selected(Collection<FlowName> flowCollection);

}
