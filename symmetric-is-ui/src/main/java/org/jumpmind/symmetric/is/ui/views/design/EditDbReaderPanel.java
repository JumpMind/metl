package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditDbReaderPanel extends VerticalLayout implements IUiPanel {

    ApplicationContext context;

    Component component;

    public EditDbReaderPanel(ApplicationContext context, Component component) {
        this.context = context;
        this.component = component;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        buttonBar.addButton("Execute", FontAwesome.PLAY, new ExecuteSqlClickListener());

    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void showing() {
    }

    class ExecuteSqlClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
        }
    }

}
