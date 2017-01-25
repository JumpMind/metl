package org.jumpmind.metl.ui.views.release;

import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditReleasePackageDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;
    
    ReleasePackage releasePackage;
    
    ApplicationContext context;
    
    IReleasePackageListener listener;

    public EditReleasePackageDialog(ReleasePackage releasePackage, ApplicationContext context, IReleasePackageListener listener) {
        super(releasePackage == null ? "New Release" : "Edit Release");
        this.releasePackage = releasePackage;
        this.listener = listener;
        this.context = context;
        
        setWidth(600.0f, Unit.PIXELS);
        setHeight(600.0f, Unit.PIXELS);
        
        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        addComponent(layout, 1);
        
        Button cancelButton = new Button("Cancel", e->cancel());
        Button saveButton = new Button("Save", e->save());
        saveButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        saveButton.setClickShortcut(KeyCode.ENTER);
        addComponent(buildButtonFooter(cancelButton, saveButton));
    }
    
    protected void save() {
        close();
    }
    
    protected void cancel() {
        close();
    }

}
