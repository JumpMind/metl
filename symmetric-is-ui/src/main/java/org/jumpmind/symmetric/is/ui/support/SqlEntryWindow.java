package org.jumpmind.symmetric.is.ui.support;

import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

public class SqlEntryWindow extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    protected AceEditor editor;

    public SqlEntryWindow(String sql) {
        setCaption("Edit SQL");
        editor = UiUtils.createAceEditor();
        editor.setMode(AceMode.sql);
        editor.setValue(sql);
        editor.setSizeFull();
        content.addComponents(editor, buildButtonFooter(null, buildCloseButton()));
        content.setExpandRatio(editor, 1);
    }
    
    public String getSQL() {
        return editor.getValue();
    }

    @Override
    protected boolean onClose() {
        return super.onClose();
    }

}
