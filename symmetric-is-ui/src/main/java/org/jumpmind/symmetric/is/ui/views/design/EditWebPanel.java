package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.runtime.component.Web;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;

@SuppressWarnings("serial")
public class EditWebPanel extends AbstractComponentEditPanel {

    AceEditor editor;
    
    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        editor = CommonUiUtils.createAceEditor();
        editor.setTextChangeEventMode(TextChangeEventMode.LAZY);
        editor.setTextChangeTimeout(200);
        editor.setMode(AceMode.xml);
        editor.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                component.put(Web.BODY_TEXT, event.getText());
                context.getConfigurationService().save(component.findSetting(Web.BODY_TEXT));
            }
        });

        addComponent(editor);
        setExpandRatio(editor, 1);
    }

}
