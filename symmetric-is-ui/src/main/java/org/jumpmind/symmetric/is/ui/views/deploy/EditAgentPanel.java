package org.jumpmind.symmetric.is.ui.views.deploy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.AbstractBeanContainer.BeanIdResolver;
import com.vaadin.data.util.BeanContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.DefaultFieldFactory;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditAgentPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Table table;

    BeanContainer<AgentDeployment, AgentDeployment> container;

    Agent agent;

    Set<Object> lastEditItemIds = Collections.emptySet();

    Button removeButton;

    Button editButton;

    public EditAgentPanel(ApplicationContext context, Agent agent) {
        this.context = context;
        this.agent = agent;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button addDeploymentButton = buttonBar.addButton("Add Deployment", Icons.DEPLOYMENT);
        addDeploymentButton.addClickListener(new AddDeploymentClickListener());

        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT);
        editButton.addClickListener(new EditClickListener());

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new RemoveClickListener());

        container = new BeanContainer<AgentDeployment, AgentDeployment>(AgentDeployment.class);
        container.setBeanIdResolver(new BeanIdResolver<AgentDeployment, AgentDeployment>() {
            @Override
            public AgentDeployment getIdForBean(AgentDeployment bean) {
                return bean;
            }
        });

        table = new Table();
        table.setSizeFull();
        table.setCacheRate(100);
        table.setPageLength(100);
        table.setImmediate(true);
        table.setSelectable(true);
        table.setMultiSelect(true);

        table.setContainerDataSource(container);
        table.setVisibleColumns(new Object[] { "flow", "status", "logLevel", "startType",
                "startExpression" });
        table.addItemClickListener(new TableItemClickListener());
        table.addValueChangeListener(new TableValueChangeListener());
        table.setTableFieldFactory(new EditableFieldFactory());

        addComponent(table);
        setExpandRatio(table, 1.0f);

        container.addAll(agent.getAgentDeployments());

        if (agent.getAgentDeployments().size() > 0) {
            table.setValue(agent.getAgentDeployments().iterator().next());
            table.focus();
        } else {
            addDeploymentButton.focus();
        }

        setButtonsEnabled();
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void showing() {
    }

    protected void setButtonsEnabled() {
        boolean selected = getSelectedItems().size() > 0;
        removeButton.setEnabled(selected);
        editButton.setEnabled(selected);
    }

    @SuppressWarnings("unchecked")
    protected Set<Object> getSelectedItems() {
        return (Set<Object>) table.getValue();
    }

    protected void selectOnly(Object itemId) {
        for (Object id : getSelectedItems()) {
            table.unselect(id);
        }
        table.select(itemId);
    }

    protected void editSelectedItem() {
        lastEditItemIds = getSelectedItems();
        table.setEditable(true);
    }

    class EditableFieldFactory extends DefaultFieldFactory {

        @Override
        public Field<?> createField(Container container, Object itemId, Object propertyId,
                Component uiContext) {
            if (lastEditItemIds.contains(itemId)) {
                if (propertyId.equals("flow")) {
                    AbstractSelect combo = new ComboBox();
                    IConfigurationService service = context.getConfigurationService();
                    List<Flow> allFlows = new ArrayList<Flow>(service.findFlows());
                    @SuppressWarnings("unchecked")
                    Collection<AgentDeployment> allDeployments = (Collection<AgentDeployment>) container
                            .getItemIds();
                    for (AgentDeployment agentDeployment : allDeployments) {
                        Iterator<Flow> i = allFlows.iterator();
                        while (i.hasNext()) {
                            Flow flow = i.next();
                            if (agentDeployment.getFlow().equals(flow)) {
                                i.remove();
                            }
                        }
                    }
                    combo.addItems(allFlows);
                    combo.focus();
                    return combo;
                } else {
                    super.createField(container, itemId, propertyId, uiContext);
                }
            }
            return null;
        }

    }

    class AddDeploymentClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            AgentDeployment deployment = new AgentDeployment();
            deployment.setAgentId(agent.getId());
            deployment.setFlow(new Flow());
            container.addItem(deployment, deployment);
            selectOnly(deployment);
            editSelectedItem();
        }
    }

    class EditClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            editSelectedItem();
        }
    }

    class RemoveClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            Set<Object> selectedIds = getSelectedItems();
            for (Object itemId : selectedIds) {
                table.removeItem(itemId);
                context.getConfigurationService().delete((AgentDeployment) itemId);
            }
        }
    }

    class TableItemClickListener implements ItemClickListener {
        public void itemClick(ItemClickEvent event) {
            if (event.isDoubleClick()) {
                editSelectedItem();
            } else {
                if (getSelectedItems().contains(event.getItemId())) {
                    table.setValue(null);
                }
            }
        }
    }

    class TableValueChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            for (Object itemId : lastEditItemIds) {
                if (itemId instanceof AgentDeployment) {
                    AgentDeployment agentDeployment = (AgentDeployment) itemId;
                    try {
                        context.getConfigurationService().save(agentDeployment);
                        agent.getAgentDeployments().add(agentDeployment);
                    } catch (Exception e1) {
                        table.removeItem(agentDeployment);
                    }
                }
            }
            table.setEditable(false);
            setButtonsEnabled();
        }
    }

}
