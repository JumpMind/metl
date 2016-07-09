package org.jumpmind.metl.ui.views.design;

import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.TempRdbms;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;

public class EditTempRdbmsPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;

    AceEditor editor;

    protected void buildUI() {
    	editor = CommonUiUtils.createAceEditor();
        editor.setTextChangeEventMode(TextChangeEventMode.LAZY);
        editor.setTextChangeTimeout(200);
        editor.setMode(AceMode.sql);
        
        Setting data = component.findSetting(TempRdbms.SQL);
        editor.setValue(data.getValue());

        editor.addTextChangeListener(new TextChangeListener() {
			private static final long serialVersionUID = 1L;

			@Override
            public void textChange(TextChangeEvent event) {
            	Setting data = component.findSetting(TempRdbms.SQL);
                data.setValue(editor.getValue());
                context.getConfigurationService().save(data);
            }
        });

        addComponent(editor);
        setExpandRatio(editor, 1);
    }

}
