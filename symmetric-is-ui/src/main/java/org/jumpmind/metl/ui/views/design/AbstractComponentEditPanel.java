package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;

import com.vaadin.ui.VerticalLayout;

public abstract class AbstractComponentEditPanel extends VerticalLayout implements IComponentEditPanel {

    private static final long serialVersionUID = 1L;
    
    protected Component component;
    
    protected ApplicationContext context;
    
    protected PropertySheet propertySheet;

    @Override
    public void init(Component component, ApplicationContext context, PropertySheet propertySheet) {
        this.component = component;
        this.context  = context;
        this.propertySheet = propertySheet;
        buildUI();
    }
    
    abstract protected void buildUI();
    
    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }
    
    
}
