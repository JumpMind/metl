package org.jumpmind.metl.ui.views.design;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.symmetric.ui.common.IUiPanel;

public interface IComponentEditPanel extends IUiPanel, com.vaadin.ui.Component {

    public void init(Component component, ApplicationContext context, PropertySheet propertySheet);

}
