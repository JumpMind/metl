package org.jumpmind.symmetric.is.ui.views.design;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.component.EntityRouter;
import org.jumpmind.symmetric.is.core.runtime.component.EntityRouter.Route;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.ItemClickEvent.ItemClickListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Field;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditEntityRouterPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    FlowStep flowStep;

    Flow flow;

    Table table = new Table();

    Button addButton;

    Button removeButton;

    BeanItemContainer<Route> container = new BeanItemContainer<Route>(Route.class);

    public EditEntityRouterPanel(ApplicationContext context, FlowStep flowStep, Flow flow) {
        this.context = context;
        this.flowStep = flowStep;
        this.flow = flow;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        addButton = buttonBar.addButton("Add", FontAwesome.PLUS);
        addButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                table.addItem(new Route());
            }
        });

        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                if (table.getValue() != null) {
                    table.removeItem(table.getValue());
                    save();
                }
            }
        });

        table.setContainerDataSource(container);

        table.setSelectable(true);
        table.setSortEnabled(false);
        table.setImmediate(true);
        table.setSizeFull();
        table.setVisibleColumns(new Object[] { "matchExpression", "targetStepId" });
        table.setColumnHeaders(new String[] { "Expression", "Target Step" });
        table.setTableFieldFactory(new EditFieldFactory());
        table.setEditable(true);
        table.addItemClickListener(new ItemClickListener() {

            @Override
            public void itemClick(ItemClickEvent event) {
                if (table.getValue() != null) {
                    table.setValue(null);
                }
            }
        });
        table.addValueChangeListener(new ValueChangeListener() {

            @Override
            public void valueChange(ValueChangeEvent event) {
                removeButton.setEnabled(table.getValue() != null);
            }
        });

        addComponent(table);
        setExpandRatio(table, 1.0f);

        String json = flowStep.getComponent().get(EntityRouter.SETTING_CONFIG);
        if (isNotBlank(json)) {
            try {
                List<Route> routes = new ObjectMapper().readValue(json,
                        new TypeReference<List<Route>>() {
                        });
                for (Route route : routes) {
                    table.addItem(route);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

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

    protected void save() {
        @SuppressWarnings("unchecked")
        List<Route> routes = new ArrayList<Route>((Collection<Route>) table.getItemIds());
        try {
            Setting setting = flowStep.getComponent().findSetting(EntityRouter.SETTING_CONFIG);
            setting.setValue(new ObjectMapper().writeValueAsString(routes));
            context.getConfigurationService().save(setting);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    class EditFieldFactory implements TableFieldFactory {
        public Field<?> createField(final Container dataContainer, final Object itemId,
                final Object propertyId, com.vaadin.ui.Component uiContext) {
            final Route route = (Route) itemId;
            Field<?> field = null;
            if (propertyId.equals("matchExpression")) {
                final TextField textField = new ImmediateUpdateTextField(null) {
                    @Override
                    protected void save() {
                        route.setMatchExpression(getValue());
                        EditEntityRouterPanel.this.save();
                    }
                };
                textField.setWidth(100, Unit.PERCENTAGE);
                textField.setValue(route.getMatchExpression());
                field = textField;
            } else if (propertyId.equals("targetStepId")) {
                final ComboBox combo = new ComboBox();
                combo.setWidth(100, Unit.PERCENTAGE);
                List<FlowStepLink> stepLinks = flow.findFlowStepLinksWithSource(flowStep.getId());
                for (FlowStepLink flowStepLink : stepLinks) {
                    FlowStep comboStep = flow.findFlowStepWithId(flowStepLink.getTargetStepId());
                    combo.addItem(comboStep.getId());
                    combo.setItemCaption(comboStep.getId(), comboStep.getName());
                    
                    if (flowStepLink.getTargetStepId().equals(route.getTargetStepId())
                            || combo.getValue() == null) {
                        combo.setValue(comboStep.getId());
                    }
                }

                combo.setImmediate(true);
                combo.setNewItemsAllowed(false);
                combo.setNullSelectionAllowed(false);
                combo.addValueChangeListener(new ValueChangeListener() {
                    public void valueChange(ValueChangeEvent event) {
                        String stepId = (String)event.getProperty().getValue();
                        if (stepId != null) {
                            route.setTargetStepId(stepId);
                            EditEntityRouterPanel.this.save();
                        }
                    }
                });
                field = combo;
            }

            return field;
        }
    }

}
