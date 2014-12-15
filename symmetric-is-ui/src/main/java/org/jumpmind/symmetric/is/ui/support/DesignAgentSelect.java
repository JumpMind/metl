package org.jumpmind.symmetric.is.ui.support;

import java.util.Set;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.ui.ComboBox;

@UiComponent
@Scope(value = "ui")
public class DesignAgentSelect extends ComboBox {

    private static final long serialVersionUID = 1L;
    
    @Autowired
    IAgentManager agentManager;
    
    public DesignAgentSelect() {
        setNewItemsAllowed(false);
        setWidth(16, Unit.EM);
        setInputPrompt("Design Time Agent");
    }
    
    @PostConstruct
    protected void init() {
    }
    
    public void refresh() {
        Object selected = getValue();
        removeAllItems();
        Set<Agent> agents = agentManager.getLocalAgents();
        for (Agent agent : agents) {
            addItem(agent);
        }
        
        if (selected != null) {
            setValue(selected);
        }
    }
}
