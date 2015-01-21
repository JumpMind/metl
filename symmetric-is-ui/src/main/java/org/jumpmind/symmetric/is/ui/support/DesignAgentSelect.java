package org.jumpmind.symmetric.is.ui.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jumpmind.symmetric.is.core.config.Agent;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.ui.common.UiComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.ui.NativeSelect;

@UiComponent
@Scope(value = "ui")
public class DesignAgentSelect extends NativeSelect {

    private static final long serialVersionUID = 1L;

    @Autowired
    IAgentManager agentManager;

    public DesignAgentSelect() {
        setNewItemsAllowed(false);
        setNullSelectionAllowed(false);
        setWidth(16, Unit.EM);
        setCaption("Selected Agent");
    }

    @PostConstruct
    protected void init() {
    }

    public void refresh() {
        Object selected = getValue();
        removeAllItems();
        List<Agent> agents = new ArrayList<>(agentManager.getLocalAgents());
        Collections.sort(agents, new Comparator<Agent>() {
            @Override
            public int compare(Agent o1, Agent o2) {
                return o1.toString().toLowerCase().compareTo(o2.toString().toLowerCase());
            }
        });
        
        for (Agent agent : agents) {
            addItem(agent);
            if (selected == null) {
                selected = agent;
            }
        }

        if (selected != null) {
            setValue(selected);
        }
    }
}
