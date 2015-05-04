package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.runtime.component.ScriptExecutor;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.CommonUiUtils;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.VerticalLayout;

public class EditScriptPanel extends VerticalLayout {

    private static final String SCRIPT_ON_ERROR = "onError(myError, allStepErrors)";
    private static final String SCRIPT_ON_SUCCESS = "onSuccess()";
    private static final String SCRIPT_ON_INIT = "onInit()";
    private static final String SCRIPT_ON_HANDLE = "onHandleMessage(inputMessage, messageTarget)";
    private static final String SCRIPT_IMPORTS = "<Imports>";

    private static final long serialVersionUID = 1L;

    Component component;

    PropertySheet propertySheet;

    ApplicationContext context;

    AceEditor editor;

    @SuppressWarnings("serial")
    public EditScriptPanel(ApplicationContext context, Component component,
            PropertySheet propertySheet) {
        this.component = component;
        this.propertySheet = propertySheet;
        this.context = context;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);
        
        editor = CommonUiUtils.createAceEditor();
        editor.setTextChangeEventMode(TextChangeEventMode.LAZY);
        editor.setTextChangeTimeout(200);

        editor.setMode(AceMode.java);

        final ComboBox select = new ComboBox();
        select.setWidth(40, Unit.EM);
        select.setTextInputAllowed(false);

        select.addItem(ScriptExecutor.HANDLE_SCRIPT);
        select.setItemCaption(ScriptExecutor.HANDLE_SCRIPT, SCRIPT_ON_HANDLE);
        select.addItem(ScriptExecutor.INIT_SCRIPT);
        select.setItemCaption(ScriptExecutor.INIT_SCRIPT, SCRIPT_ON_INIT);
        select.addItem(ScriptExecutor.ON_FLOW_SUCCESS);
        select.setItemCaption(ScriptExecutor.ON_FLOW_SUCCESS, SCRIPT_ON_SUCCESS);
        select.addItem(ScriptExecutor.ON_FLOW_ERROR);
        select.setItemCaption(ScriptExecutor.ON_FLOW_ERROR, SCRIPT_ON_ERROR);
        select.addItem(ScriptExecutor.IMPORTS);
        select.setItemCaption(ScriptExecutor.IMPORTS, SCRIPT_IMPORTS);

        select.setImmediate(true);
        select.setNullSelectionAllowed(false);
        select.setNewItemsAllowed(false);
        select.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                editor.setValue(EditScriptPanel.this.component.get((String) select.getValue(), ""));
            }
        });
        select.setValue(ScriptExecutor.HANDLE_SCRIPT);
        buttonBar.addLeft(select);

        editor.addTextChangeListener(new TextChangeListener() {

            @Override
            public void textChange(TextChangeEvent event) {
                String key = (String) select.getValue();
                EditScriptPanel.this.component.put(key, event.getText());
                EditScriptPanel.this.context.getConfigurationService().save(
                        EditScriptPanel.this.component.findSetting(key));
            }
        });
        
        addComponent(editor);
        setExpandRatio(editor, 1);

    }
}
