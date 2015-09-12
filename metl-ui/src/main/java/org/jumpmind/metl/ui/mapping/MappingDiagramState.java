package org.jumpmind.metl.ui.mapping;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.Model;
import org.jumpmind.metl.core.runtime.component.Mapping;

import com.vaadin.shared.ui.JavaScriptComponentState;

public class MappingDiagramState extends JavaScriptComponentState {

    private static final long serialVersionUID = 1L;
    
    public String mapsToAttrName = Mapping.ATTRIBUTE_MAPS_TO;

    public Component component;
    
    public Model inputModel;
    
    public Model outputModel;

}
