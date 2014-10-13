package org.jumpmind.symmetric.is.ui.views.flows;

import java.util.List;

import org.jumpmind.symmetric.is.core.config.ComponentFlowVersion;
import org.jumpmind.symmetric.is.core.persist.IConfigurationService;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.jumpmind.symmetric.is.ui.support.ResizableWindow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Button;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@Component
@Scope(value = "ui")
public class EditFlowWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    @Autowired
    IConfigurationService configurationService;
    
    @Autowired
    IComponentFactory componentFactory;

    ComponentFlowVersion flowVersion;
    
    VerticalLayout componentLayout;
    
    CssLayout flowLayout;

    public EditFlowWindow() {

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setSplitPosition(260, Unit.PIXELS);
        splitPanel.setSizeFull();

        flowLayout = new CssLayout();

        splitPanel.addComponents(buildPalette(), flowLayout);
        
        content.addComponent(splitPanel);
        content.setExpandRatio(splitPanel, 1);
        content.addComponent(buildFooter());

    }
    
    protected VerticalLayout buildPalette() {
        VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        layout.setSizeFull();
        layout.setSpacing(true);
        
        TextField tf = new TextField();
        tf.setInputPrompt("Search Components");
        tf.addStyleName("small");
        tf.addStyleName("inline-icon");
        tf.setIcon(FontAwesome.SEARCH);
        layout.addComponent(tf);
        
        Accordion accordian = new Accordion();
        accordian.setSizeFull();
        componentLayout = new VerticalLayout();        
        accordian.addTab(componentLayout, "Components", FontAwesome.SITEMAP);
        layout.addComponent(accordian);
        layout.setExpandRatio(accordian, 1);
        return layout;
        
    }

    protected HorizontalLayout buildFooter() {
        HorizontalLayout footer = new HorizontalLayout();

        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);

        Label footerText = new Label("");
        footerText.setSizeUndefined();

        Button ok = new Button("OK");
        ok.addStyleName("primary");

        Button cancel = new Button("Cancel");
        cancel.addClickListener(new CancelButtonListener());

        footer.addComponents(footerText, cancel, ok);
        footer.setExpandRatio(footerText, 1);
        return footer;
    }

    public void show(ComponentFlowVersion flowVersion) {
        this.flowVersion = flowVersion;
        setCaption("Edit Flow: " + flowVersion.getFlow().getData().getName());

        componentLayout.removeAllComponents();
        List<String> componentTypes = componentFactory.getComponentTypes();
        for (String componentType : componentTypes) {
            Button button = new Button(componentType);
            button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
            button.setWidth(100, Unit.PERCENTAGE);
            componentLayout.addComponent(button);
        }
        
        resize(.8, true);

    }
    
}
