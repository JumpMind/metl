package org.jumpmind.symmetric.is.ui.views.deploy;

import java.util.Collection;

import org.jumpmind.symmetric.is.core.model.Flow;

public interface FlowSelectListener {

    public void selected(Collection<Flow> flowCollection);

}
