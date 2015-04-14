package org.jumpmind.symmetric.is.ui.views.design;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.jumpmind.symmetric.is.core.model.AbstractObject;
import org.jumpmind.symmetric.is.core.model.Agent;
import org.jumpmind.symmetric.is.core.model.AgentDeployment;
import org.jumpmind.symmetric.is.core.model.AgentStartMode;
import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.Flow;
import org.jumpmind.symmetric.is.core.model.FlowParameter;
import org.jumpmind.symmetric.is.core.model.FlowStep;
import org.jumpmind.symmetric.is.core.model.FlowStepLink;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.IAgentManager;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentDefinition;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.is.ui.common.IBackgroundRefreshable;
import org.jumpmind.symmetric.is.ui.common.Icons;
import org.jumpmind.symmetric.is.ui.common.TabbedPanel;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.diagram.LinkEvent;
import org.jumpmind.symmetric.is.ui.diagram.LinkSelectedEvent;
import org.jumpmind.symmetric.is.ui.diagram.Node;
import org.jumpmind.symmetric.is.ui.diagram.NodeDoubleClickedEvent;
import org.jumpmind.symmetric.is.ui.diagram.NodeMovedEvent;
import org.jumpmind.symmetric.is.ui.diagram.NodeSelectedEvent;
import org.jumpmind.symmetric.is.ui.views.IDesignNavigator;
import org.jumpmind.symmetric.is.ui.views.manage.ExecutionLogPanel;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ImmediateUpdateTextField;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Container;
import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.dd.DragAndDropEvent;
import com.vaadin.event.dd.acceptcriteria.AcceptAll;
import com.vaadin.event.dd.acceptcriteria.AcceptCriterion;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Resource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.WrapperTargetDetails;
import com.vaadin.ui.DragAndDropWrapper.WrapperTransferable;
import com.vaadin.ui.Field;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Table;
import com.vaadin.ui.TableFieldFactory;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditFlowPanel extends HorizontalLayout implements IUiPanel, IBackgroundRefreshable {

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    Flow flow;

    PropertySheet propertySheet;

    IDesignNavigator designNavigator;

    EditFlowPalette componentPalette;

    TabbedPanel tabs;

    Diagram diagram;

    CssLayout diagramLayout;

    Button runButton;

    Button delButton;

    Button parametersButton;

    AbstractObject selected;

    public EditFlowPanel(ApplicationContext context, Flow flow,
            IDesignNavigator designNavigator, TabbedPanel tabs) {
        context.getConfigurationService().refresh(flow);
        this.flow = flow;
        this.context = context;
        this.tabs = tabs;
        this.designNavigator = designNavigator;

        this.propertySheet = new PropertySheet(context);
        this.propertySheet.setCaption("Property Sheet");

        this.componentPalette = new EditFlowPalette(this, context.getComponentFactory());

        addComponent(componentPalette);

        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setSizeFull();

        rightLayout.addComponent(buildButtonBar());

        VerticalSplitPanel splitPanel = new VerticalSplitPanel();
        splitPanel.setSizeFull();
        splitPanel.setSplitPosition(50, Unit.PERCENTAGE);

        diagramLayout = new CssLayout();
        diagramLayout.setWidth(10000, Unit.PIXELS);
        diagramLayout.setHeight(10000, Unit.PIXELS);

        DragAndDropWrapper wrapper = new DragAndDropWrapper(diagramLayout);
        wrapper.setSizeUndefined();
        wrapper.setDropHandler(new DropHandler());

        Panel panel = new Panel();
        panel.setSizeFull();
        panel.addStyleName(ValoTheme.PANEL_WELL);
        panel.setContent(wrapper);

        splitPanel.addComponent(panel);
        splitPanel.addComponent(propertySheet);

        rightLayout.addComponent(splitPanel);
        rightLayout.setExpandRatio(splitPanel, 1);

        addComponent(rightLayout);
        setExpandRatio(rightLayout, 1);

        redrawFlow();

        context.getBackgroundRefresherService().register(this);
    }

    protected HorizontalLayout buildButtonBar() {
        ButtonBar buttonBar = new ButtonBar();
        runButton = buttonBar.addButton("Run", Icons.RUN);
        runButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                runFlow();
            }
        });

        delButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        delButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                deleteSelected();
            }
        });
        delButton.setEnabled(false);

        parametersButton = buttonBar.addButton("Parameters", FontAwesome.LIST_OL);
        parametersButton.addClickListener(new ClickListener() {

            @Override
            public void buttonClick(ClickEvent event) {
                new EditParametersWindow().showAtSize(.50);
            }
        });

        return buttonBar;
    }

    protected Button createToolButton(String name, Resource icon) {
        Button button = new Button(name, icon);
        button.addStyleName(ValoTheme.BUTTON_SMALL);
        button.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        return button;
    }

    @Override
    public void onBackgroundUIRefresh(Object backgroundData) {
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object onBackgroundDataRefresh() {
        return null;
    }

    @Override
    public boolean closing() {
        context.getBackgroundRefresherService().unregister(this);
        designNavigator.setPropertySheet(null);
        return true;
    }

    @Override
    public void selected() {
        designNavigator.setPropertySheet(propertySheet);
    }

    public Flow getFlow() {
        return flow;
    }

    public void selected(FlowStep step) {
        if (step != null) {
            diagram.setNodes(getNodes());
            diagram.setSelectedNodeId(step.getId());
            propertySheet.valueChange(step);
        } else {
            diagram.setSelectedNodeId(null);
            propertySheet.valueChange((Object) null);
        }
    }

    @Override
    public void deselected() {
    }

    protected void deleteSelected() {
        IConfigurationService configurationService = context.getConfigurationService();
        if (selected instanceof FlowStep) {
            FlowStep flowStep = (FlowStep) selected;
            configurationService.delete(flow, flowStep);
            designNavigator.refresh();
            redrawFlow();
        } else if (selected instanceof FlowStepLink) {
            FlowStepLink link = (FlowStepLink) selected;
            configurationService.delete(link);
            flow.removeFlowStepLink(link.getSourceStepId(), link.getTargetStepId());
            redrawFlow();
        }
    }

    protected int countComponentsOfType(String type) {
        int count = 0;
        List<FlowStep> nodes = flow.getFlowSteps();
        for (FlowStep componentflowStep : nodes) {
            if (componentflowStep.getComponent().getType().equals(type)) {
                count++;
            }
        }
        return count;
    }

    protected void addComponent(int x, int y, Component component) {

        component.setName(component.getType() + " "
                + (countComponentsOfType(component.getType()) + 1));

        FlowStep flowStep = new FlowStep(component);
        flowStep.setX(x);
        flowStep.setY(y);
        flowStep.setFlowId(flow.getId());
        flow.getFlowSteps().add(flowStep);

        context.getConfigurationService().save(flowStep);

        redrawFlow();

        propertySheet.valueChange(component);

        designNavigator.refresh();
        designNavigator.select(flowStep);

    }

    protected void redrawFlow() {
        selected = null;
        delButton.setEnabled(false);
        if (diagram != null) {
            diagramLayout.removeComponent(diagram);
        }

        diagram = new Diagram();
        diagram.setSizeFull();
        diagram.addListener(new DiagramChangedListener());

        diagramLayout.addComponent(diagram);

        diagram.setNodes(getNodes());
    }

    protected List<Node> getNodes() {
        List<FlowStep> flowSteps = flow.getFlowSteps();
        List<FlowStepLink> links = flow.getFlowStepLinks();
        List<Node> list = new ArrayList<Node>();
        for (FlowStep flowStep : flowSteps) {
            Node node = new Node();
            String name = flowStep.getComponent().getName();
            String type = flowStep.getComponent().getType();
            String imageText = String
                    .format("<img style=\"display: block; margin-left: auto; margin-right: auto\" src=\"data:image/png;base64,%s\"/>",
                            componentPalette.getBase64RepresentationOfImageForComponentType(type));
            node.setText(imageText + "<br><i>" + name + "</i>");
            node.setId(flowStep.getId());
            node.setX(flowStep.getX());
            node.setY(flowStep.getY());

            ComponentDefinition definition = context.getComponentFactory()
                    .getComponentDefinitionForComponentType(type);
            node.setInputLabel(definition.inputMessage().getLetter());
            node.setOutputLabel(definition.outgoingMessage().getLetter());

            for (FlowStepLink link : links) {
                if (link.getSourceStepId().equals(node.getId())) {
                    node.getTargetNodeIds().add(link.getTargetStepId());
                }
            }

            list.add(node);

        }
        return list;
    }

    protected void runFlow() {
        IAgentManager agentManager = context.getAgentManager();
        Set<Agent> agents = agentManager.getLocalAgents();
        Agent localAgent = null;
        for (Agent agent : agents) {
            if (agent.getHost().equals("localhost")) {
                localAgent = agent;
                break;
            }
        }

        if (localAgent == null) {
            localAgent = new Agent();
            localAgent.setHost("localhost");
            localAgent.setName("local");
            localAgent.setStartMode(AgentStartMode.AUTO.name());
            context.getConfigurationService().save(localAgent);
            agentManager.refresh(localAgent);
        }

        AgentDeployment deployment = localAgent.getAgentDeploymentFor(flow);
        if (deployment != null) {
            agentManager.undeploy(deployment);

        }

        deployment = agentManager.deploy(localAgent.getId(), flow, new HashMap<String, String>());

        String executionId = agentManager.getAgentRuntime(localAgent).scheduleNow(deployment);
        if (executionId != null) {
            ExecutionLogPanel logPanel = new ExecutionLogPanel(executionId, context);
            tabs.addCloseableTab(executionId, "Run " + flow.getName(), Icons.LOG, logPanel);
        }
    }

    class DiagramChangedListener implements Listener {

        @Override
        public void componentEvent(Event e) {
            IConfigurationService configurationService = context.getConfigurationService();
            if (e instanceof NodeSelectedEvent) {
                NodeSelectedEvent event = (NodeSelectedEvent) e;
                Node node = event.getNode();
                FlowStep flowStep = flow.findFlowStepWithId(node.getId());
                designNavigator.select(flowStep);
                selected = flowStep;
                delButton.setEnabled(true);
            } else if (e instanceof NodeDoubleClickedEvent) {
                NodeDoubleClickedEvent event = (NodeDoubleClickedEvent) e;
                Node node = event.getNode();
                FlowStep flowStep = flow.findFlowStepWithId(node.getId());
                designNavigator.open(flowStep);
            } else if (e instanceof NodeMovedEvent) {
                NodeMovedEvent event = (NodeMovedEvent) e;
                Node node = event.getNode();
                FlowStep flowStep = flow.findFlowStepWithId(node.getId());
                if (flowStep != null) {
                    flowStep.setX(node.getX());
                    flowStep.setY(node.getY());
                }
                configurationService.save(flow);
            } else if (e instanceof LinkEvent) {
                LinkEvent event = (LinkEvent) e;
                if (!event.isRemoved()) {
                    flow.getFlowStepLinks().add(
                            new FlowStepLink(event.getSourceNodeId(), event.getTargetNodeId()));
                    configurationService.save(flow);
                } else {
                    FlowStepLink link = flow.removeFlowStepLink(event.getSourceNodeId(),
                            event.getTargetNodeId());
                    if (link != null) {
                        configurationService.delete(link);
                        redrawFlow();
                    }
                }
            } else if (e instanceof LinkSelectedEvent) {
                LinkSelectedEvent event = (LinkSelectedEvent) e;
                selected = flow.findFlowStepLink(event.getSourceNodeId(), event.getTargetNodeId());
                delButton.setEnabled(true);
            }
        }
    }

    class DropHandler implements com.vaadin.event.dd.DropHandler {

        @Override
        public void drop(DragAndDropEvent event) {
            WrapperTransferable t = (WrapperTransferable) event.getTransferable();
            WrapperTargetDetails details = (WrapperTargetDetails) event.getTargetDetails();
            DragAndDropWrapper wrapper = (DragAndDropWrapper) t.getSourceComponent();
            Button button = (Button) wrapper.iterator().next();
            Component component = new Component();
            component.setType(button.getCaption());
            component.setShared(false);
            addComponent(details.getMouseEvent().getClientX() - details.getAbsoluteLeft(), details
                    .getMouseEvent().getClientY() - details.getAbsoluteTop(), component);
        }

        @Override
        public AcceptCriterion getAcceptCriterion() {
            return AcceptAll.get();
        }
    }

    class EditParametersWindow extends ResizableWindow {

        Table table;

        public EditParametersWindow() {
            super("Flow Parameters");
            Button closeButton = new Button("Close");
            closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
            closeButton.addClickListener(new ClickListener() {

                @Override
                public void buttonClick(ClickEvent event) {
                    EditParametersWindow.this.close();
                }
            });

            ButtonBar buttonBar = new ButtonBar();
            buttonBar.addButton("Add", FontAwesome.PLUS, new ClickListener() {

                @Override
                public void buttonClick(ClickEvent event) {
                    FlowParameter parameter = new FlowParameter();
                    parameter.setFlowId(flow.getId());
                    parameter.setName("Parameter " + (flow.getFlowParameters().size() + 1));
                    parameter.setPosition(flow.getFlowParameters().size() + 1);
                    context.getConfigurationService().save(parameter);
                    flow.getFlowParameters().add(parameter);
                    table.addItem(parameter);

                }
            });
            final Button removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O,
                    new ClickListener() {

                        @Override
                        public void buttonClick(ClickEvent event) {
                            FlowParameter parameter = (FlowParameter) table.getValue();
                            if (parameter != null) {
                                flow.getFlowParameters().remove(parameter);
                                context.getConfigurationService()
                                        .delete((AbstractObject) parameter);
                                table.removeItem(parameter);

                                @SuppressWarnings("unchecked")
                                Collection<FlowParameter> parameters = (Collection<FlowParameter>) table
                                        .getItemIds();
                                int count = 1;
                                for (FlowParameter p : parameters) {
                                    p.setPosition(count++);
                                    context.getConfigurationService().save(p);
                                }
                            }
                        }
                    });
            removeButton.setEnabled(false);
            addComponent(buttonBar);

            table = new Table();
            table.setSizeFull();
            BeanItemContainer<FlowParameter> container = new BeanItemContainer<FlowParameter>(
                    FlowParameter.class);
            table.setContainerDataSource(container);
            table.setEditable(true);
            table.setSelectable(true);
            table.setTableFieldFactory(new EditFieldFactory());
            table.setVisibleColumns("name", "defaultValue");
            table.setColumnHeaders("Name", "Default Value");
            table.addValueChangeListener(new ValueChangeListener() {

                @Override
                public void valueChange(ValueChangeEvent event) {
                    removeButton.setEnabled(table.getValue() != null);
                }
            });
            addComponent(table, 1);

            addComponent(buildButtonFooter(closeButton));

            List<FlowParameter> params = flow.getFlowParameters();
            Collections.sort(params, new Comparator<FlowParameter>() {
                @Override
                public int compare(FlowParameter o1, FlowParameter o2) {
                    return new Integer(o1.getPosition()).compareTo(new Integer(o2.getPosition()));
                }
            });

            for (FlowParameter flowParameter : params) {
                table.addItem(flowParameter);
            }

        }

        class EditFieldFactory implements TableFieldFactory {
            public Field<?> createField(final Container dataContainer, final Object itemId,
                    final Object propertyId, com.vaadin.ui.Component uiContext) {
                final FlowParameter parameter = (FlowParameter) itemId;
                final TextField textField = new ImmediateUpdateTextField(null) {
                    @Override
                    protected void save() {
                        context.getConfigurationService().save(parameter);
                    }
                };
                textField.setWidth(100, Unit.PERCENTAGE);
                return textField;
            }
        }

    }

}
