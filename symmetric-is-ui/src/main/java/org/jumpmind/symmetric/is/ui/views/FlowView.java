package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.diagram.Node;
import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.ViewLink;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.VerticalLayout;

@Component
@Scope(value = "ui")
@ViewLink(category = Category.INTEGRATIONS, name = "Flows", id = "flows", icon = FontAwesome.SHARE_ALT, menuOrder = 10)
public class FlowView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;
   
    Diagram diagram;
    
    public FlowView() {
        setSizeFull();
        setMargin(true);

        Button button = new Button("Button");
        button.addClickListener(new Button.ClickListener() {
            private static final long serialVersionUID = 1L;

            public void buttonClick(ClickEvent event) {
                diagram.addNode(new Node("Test " + diagram.getNodes().size()+1));
            }
        });
        
        HorizontalLayout buttons = new HorizontalLayout();
        buttons.setMargin(true);
        buttons.addComponent(button);

        addComponent(buttons);
        
        diagram = new Diagram();
        diagram.setSizeFull();
        
        addComponent(diagram);
        setExpandRatio(diagram, 1);
    }

    @Override
    public void enter(ViewChangeEvent event) {
    }

}
