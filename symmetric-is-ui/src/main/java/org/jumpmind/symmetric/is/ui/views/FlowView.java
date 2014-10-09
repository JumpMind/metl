package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.core.config.data.FolderType;
import org.jumpmind.symmetric.is.ui.diagram.Diagram;
import org.jumpmind.symmetric.is.ui.support.AbstractFolderEditPanel;
import org.jumpmind.symmetric.is.ui.support.Category;
import org.jumpmind.symmetric.is.ui.support.ViewLink;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.MenuBar;
import com.vaadin.ui.MenuBar.Command;
import com.vaadin.ui.MenuBar.MenuItem;

@Component
@Scope(value = "ui")
@ViewLink(category = Category.DESIGN, name = "Flows", id = "flows", icon = FontAwesome.SHARE_ALT, menuOrder = 10)
public class FlowView extends AbstractFolderEditPanel implements View {

    private static final long serialVersionUID = 1L;
   
    Diagram diagram;
    
    MenuItem addFlowButton;
    
    public FlowView() {
        super("Flows", FolderType.DESIGN);
    }
    
    @Override
    protected void addToAddButton(MenuBar.MenuItem dropdown) {
        addFlowButton = dropdown.addItem("Flow", FontAwesome.SHARE_ALT, new Command() {

            private static final long serialVersionUID = 1L;

            @Override
            public void menuSelected(MenuItem selectedItem) {
            }
        });
    }
    
    @Override
    protected void treeSelectionChanged(ValueChangeEvent event) {
        super.treeSelectionChanged(event);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

}
