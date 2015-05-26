package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

public interface IComponentEditPanel extends IUiPanel {

    public void init(Component component, ApplicationContext context, PropertySheet propertySheet);

}
