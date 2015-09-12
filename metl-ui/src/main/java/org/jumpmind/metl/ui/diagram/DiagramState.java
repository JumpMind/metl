package org.jumpmind.metl.ui.diagram;

import java.util.ArrayList;
import java.util.List;

import com.vaadin.shared.ui.JavaScriptComponentState;

public class DiagramState extends JavaScriptComponentState {

    private static final long serialVersionUID = 1L;
    
    public List<Node> nodes = new ArrayList<Node>();
    
    public String selectedNodeId;
    
}
