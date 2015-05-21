package org.jumpmind.symmetric.is.ui.views.design;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.IUiPanel;
import org.jumpmind.symmetric.ui.common.ResizableWindow;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.Label;
import com.vaadin.ui.TextArea;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings({ "serial" })
public class EditXsltPanel extends VerticalLayout implements IUiPanel, TextChangeListener {

    ApplicationContext context;

    Component component;

    TextField filterField;
    
    AceEditor editor;
    
    TextArea textArea;

    public EditXsltPanel(ApplicationContext context, Component component) {
        this.context = context;
        this.component = component;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        Button testButton = buttonBar.addButton("Test", FontAwesome.FILE_CODE_O);
        testButton.addClickListener(new TestClickListener());

        filterField = buttonBar.addFilter();
        filterField.addTextChangeListener(this);
        
        HorizontalSplitPanel splitPanel = new HorizontalSplitPanel();
        splitPanel.setSizeFull();
        splitPanel.setSplitPosition(50, Unit.PERCENTAGE);

        VerticalLayout leftLayout = new VerticalLayout();
        editor = new AceEditor();
        editor.setMode(AceMode.xml);
        editor.setSizeFull();
        editor.setHighlightActiveLine(true);
        editor.setShowPrintMargin(false);
        leftLayout.addComponent(new Label("XSLT Stylesheet"));
        leftLayout.addComponent(editor);
        leftLayout.setExpandRatio(editor, 1.0f);
        leftLayout.setSizeFull();
        splitPanel.setFirstComponent(leftLayout);
        
        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.addComponent(new Label("Sample Input XML"));
        textArea = new TextArea();
        textArea.setEnabled(false);
        textArea.setSizeFull();
        textArea.setValue("<batch>\n<entity>\n<attribute>hi</attribute>\n</entity>\n</batch>");
        rightLayout.addComponent(textArea);
        rightLayout.setExpandRatio(textArea, 1.0f);
        splitPanel.setSecondComponent(rightLayout);

        addComponent(splitPanel);
        setExpandRatio(splitPanel, 1.0f);

        updateTable(null);
    }

    @Override
    public boolean closing() {
        return true;
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }

    @Override
    public void textChange(TextChangeEvent event) {
        filterField.setValue(event.getText());
        updateTable(event.getText());
    }

    protected void updateTable(String filter) {
    }

    class TestClickListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            TestWindow window = new TestWindow();
            window.show();
        }
    }

    class TestWindow extends ResizableWindow {
        public TestWindow() {
            super("Test Transformation");
            setWidth(600f, Unit.PIXELS);
            setHeight(300f, Unit.PIXELS);
            content.setMargin(true);
            
            TextArea textField = new TextArea();
            textField.setSizeFull();
            textField.setWordwrap(false);
            textField.setValue("Message goes here");
            addComponent(textField);
            content.setExpandRatio(textField, 1.0f);
            
            addComponent(buildButtonFooter(buildCloseButton()));
        }
    }

}
