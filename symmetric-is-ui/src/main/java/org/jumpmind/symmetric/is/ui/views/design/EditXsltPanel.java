package org.jumpmind.symmetric.is.ui.views.design;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jumpmind.symmetric.is.core.model.DataType;
import org.jumpmind.symmetric.is.core.model.Model;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.model.Setting;
import org.jumpmind.symmetric.is.core.runtime.EntityData;
import org.jumpmind.symmetric.is.core.runtime.component.XsltProcessor;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
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
public class EditXsltPanel extends AbstractComponentEditPanel implements TextChangeListener {

    TextField filterField;
    
    AceEditor editor;
    
    TextArea textArea;

    public EditXsltPanel() {
    }
    
    protected void buildUI() {

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
        editor.addTextChangeListener(new StylesheetChangeListener());
        editor.setValue(component.findSetting(XsltProcessor.XSLT_PROCESSOR_STYLESHEET).getValue());
        leftLayout.addComponent(new Label("XSLT Stylesheet"));
        leftLayout.addComponent(editor);
        leftLayout.setExpandRatio(editor, 1.0f);
        leftLayout.setSizeFull();
        splitPanel.setFirstComponent(leftLayout);
        
        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setSizeFull();
        rightLayout.addComponent(new Label("Sample Input XML"));
        textArea = new TextArea();
        textArea.setEnabled(false);
        textArea.setSizeFull();
        textArea.setValue(getSampleXml());
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
        textArea.setValue(getSampleXml());
        updateTable(event.getText());
    }

    protected void updateTable(String filter) {
    }

    protected String getSampleXml() {
        Model model = component.getInputModel();
        String batchXml = "";
        if (model != null) {
            ArrayList<EntityData> inputRows = new ArrayList<EntityData>();
            for (ModelEntity entity : getMatchingEntities(filterField.getValue())) {
                EntityData data = new EntityData();
                inputRows.add(data);
                for (ModelAttribute attr : entity.getModelAttributes()) {
                    DataType type = attr.getDataType();
                    Object value = null;
                    if (type.isString()) {
                        value = RandomStringUtils.randomAlphanumeric(10);
                    } else if (type.isNumeric()) {
                        value = String.valueOf(RandomUtils.nextInt());
                    } else if (type.isTimestamp()) {
                        value = new Date(RandomUtils.nextInt(Integer.MAX_VALUE) * 1000l);
                    } else if (type.isBoolean()) {
                        value = Boolean.toString(RandomUtils.nextBoolean());
                    }
                    data.put(attr.getId(), value);
                }
            }
            batchXml = XsltProcessor.getBatchXml(model, inputRows, false);
        }
        return batchXml;
    }

    protected List<ModelEntity> getMatchingEntities(String filter) {
        List<ModelEntity> entities = new ArrayList<ModelEntity>();
        if (component.getInputModel() != null) {
            if (StringUtils.isEmpty(filter)) {
                entities.addAll(component.getInputModel().getModelEntities());
            } else {
                String filterText = filter.toUpperCase();
                for (ModelEntity entity : component.getInputModel().getModelEntities()) {
                    if (entity.getName().toUpperCase().indexOf(filterText) != -1) {
                        entities.add(entity);
                        continue;
                    }
                    for (ModelAttribute attr : entity.getModelAttributes()) {
                        if (attr.getName().toUpperCase().indexOf(filterText) != -1) {
                            entities.add(entity);
                            break;
                        }
                    }
                }                
            }
        }
        return entities;
    }

    class StylesheetChangeListener implements TextChangeListener {
        public void textChange(TextChangeEvent event) {
            Setting stylesheet = component.findSetting(XsltProcessor.XSLT_PROCESSOR_STYLESHEET);
            stylesheet.setValue(editor.getValue());
            context.getConfigurationService().save(component);
        }
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
            setWidth(800f, Unit.PIXELS);
            setHeight(600f, Unit.PIXELS);
            content.setMargin(true);
            
            TextArea textField = new TextArea();
            textField.setSizeFull();
            textField.setWordwrap(false);
            textField.setValue(XsltProcessor.getTransformedXml(textArea.getValue(), editor.getValue(), XsltProcessor.PRETTY_FORMAT));
            addComponent(textField);
            content.setExpandRatio(textField, 1.0f);
            
            addComponent(buildButtonFooter(buildCloseButton()));
        }
    }

}
