package org.jumpmind.metl.ui.views.design;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.dbcp.BasicDataSource;
import org.jumpmind.metl.core.model.AbstractObject;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.component.RdbmsReader;
import org.jumpmind.metl.core.runtime.component.TextConstant;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;

@SuppressWarnings("serial")
public class EditTextConstant extends AbstractComponentEditPanel {

    AceEditor editor;
    
    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        editor = CommonUiUtils.createAceEditor();
        editor.setTextChangeEventMode(TextChangeEventMode.LAZY);
        editor.setTextChangeTimeout(200);
        editor.setMode(AceMode.text);
        editor.setValue(component.get(TextConstant.SETTING_TEXT));
        editor.addTextChangeListener(new TextChangeListener() {
            public void textChange(TextChangeEvent event) {
                Setting data = component.findSetting(TextConstant.SETTING_TEXT);
                data.setValue(event.getText());
                context.getConfigurationService().save(data);
            }
        });

        addComponent(editor);
        setExpandRatio(editor, 1);
    }
    
}
