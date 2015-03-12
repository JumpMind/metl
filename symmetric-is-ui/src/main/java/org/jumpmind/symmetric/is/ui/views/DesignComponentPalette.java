package org.jumpmind.symmetric.is.ui.views;

import java.util.List;
import java.util.Map;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.runtime.component.ComponentCategory;
import org.jumpmind.symmetric.is.core.runtime.component.IComponentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.DragStartMode;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class DesignComponentPalette extends Panel {

    private static final long serialVersionUID = 1L;
    
    final Logger log = LoggerFactory.getLogger(getClass());

    Accordion componentAccordian;

    IComponentFactory componentFactory;

    DesignFlowLayout designFlowLayout;

    float splitPosition = 60;

    Unit splitUnit = Unit.PERCENTAGE;

    public DesignComponentPalette(DesignFlowLayout designFlowLayout,
            IComponentFactory componentFactory) {
        this.componentFactory = componentFactory;
        this.designFlowLayout = designFlowLayout;
        setHeight(100, Unit.PERCENTAGE);
        setWidth(150, Unit.PIXELS);
        addStyleName("noborder");

        VerticalLayout content = new VerticalLayout();
        content.setSizeFull();
        setContent(content);

        componentAccordian = new Accordion();
        componentAccordian.setSizeFull();
        content.addComponent(componentAccordian);
        content.setExpandRatio(componentAccordian, 1);

        addStyleName(ValoTheme.MENU_ROOT);

        populateComponentPalette();

    }

    protected void populateComponentPalette() {
        componentAccordian.removeAllComponents();
        Map<ComponentCategory, List<String>> componentTypesByCategory = componentFactory
                .getComponentTypes();
        for (ComponentCategory category : componentTypesByCategory.keySet()) {
            List<String> componentTypes = componentTypesByCategory.get(category);
            VerticalLayout componentLayout = new VerticalLayout();
            componentAccordian.addTab(componentLayout, category.name() + "S");
            if (componentTypes != null) {
                for (String componentType : componentTypes) {
                    Button button = new Button(componentType);
                    button.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
                    button.addStyleName("leftAligned");
                    button.setWidth(100, Unit.PERCENTAGE);
                    button.addClickListener(new AddComponentClickListener(componentType));
                    DragAndDropWrapper wrapper = new DragAndDropWrapper(button); 
                    wrapper.setSizeUndefined();
                    wrapper.setDragStartMode(DragStartMode.WRAPPER);
                    componentLayout.addComponent(wrapper);
                    componentLayout.setComponentAlignment(wrapper, Alignment.TOP_CENTER);
                }
            }
        }
    }

    class AddComponentClickListener implements ClickListener {

        private static final long serialVersionUID = 1L;

        String type;

        public AddComponentClickListener(String type) {
            this.type = type;
        }

        @Override
        public void buttonClick(ClickEvent event) {
            Component component = new Component();
            component.setType(type);
            component.setShared(false);

            if (designFlowLayout != null) {
                designFlowLayout.addComponent(0, 0, component);
            }

        }
    }

}
