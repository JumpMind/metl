/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jumpmind.metl.ui.views.design;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.jumpmind.metl.core.model.DataType;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.model.ModelAttrib;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.model.Setting;
import org.jumpmind.metl.core.runtime.EntityData;
import org.jumpmind.metl.core.runtime.component.XsltProcessor;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.ResizableDialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.events.AceValueChanged;

@SuppressWarnings({ "serial" })
public class EditXsltPanel extends AbstractComponentEditPanel implements ValueChangeListener<ValueChangeEvent<String>> {

    TextField filterField;
    
    AceEditor editor;
    
    TextArea textArea;

    public EditXsltPanel() {
    }
    
    protected void buildUI() {
        setPadding(false);
        setSpacing(false);

        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);

        if (!readOnly) {
          Button testButton = buttonBar.addButton("Test", VaadinIcon.FILE_CODE);
          testButton.addClickListener(new TestClickListener());
        }

        filterField = buttonBar.addFilter();
        filterField.addValueChangeListener(this);
        
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(50);

        VerticalLayout leftLayout = new VerticalLayout();
        editor = new AceEditor();
        editor.setMode(AceMode.xml);
        editor.setSizeFull();
        editor.setHighlightActiveLine(true);
        editor.setShowPrintMargin(false);
        editor.addValueChangeListener(new StylesheetChangeListener());
        editor.setValue(component.findSetting(XsltProcessor.XSLT_PROCESSOR_STYLESHEET).getValue());
        leftLayout.add(new Span("XSLT Stylesheet"), editor);
        leftLayout.expand(editor);
        leftLayout.setSizeFull();
        splitLayout.addToPrimary(leftLayout);
        
        VerticalLayout rightLayout = new VerticalLayout();
        rightLayout.setSizeFull();
        rightLayout.add(new Span("Sample Input XML"));
        textArea = new TextArea();
        textArea.setEnabled(false);
        textArea.setSizeFull();
        textArea.setValue(getSampleXml());
        rightLayout.addAndExpand(textArea);
        splitLayout.addToSecondary(rightLayout);

        add(splitLayout);
        expand(splitLayout);
        
        textArea.setReadOnly(readOnly);
        editor.setReadOnly(readOnly);

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
    public void valueChanged(ValueChangeEvent<String> event) {
        textArea.setReadOnly(false);
        filterField.setValue(event.getValue());
        textArea.setValue(getSampleXml());
        textArea.setReadOnly(readOnly);
    }



    protected String getSampleXml() {
        RelationalModel model = (RelationalModel) component.getInputModel();
        String batchXml = "";
        if (model != null) {
            ArrayList<EntityData> inputRows = new ArrayList<EntityData>();
            for (ModelEntity entity : getMatchingEntities(filterField.getValue())) {
                EntityData data = new EntityData();
                inputRows.add(data);
                for (ModelAttrib attr : entity.getModelAttributes()) {
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
                entities.addAll(((RelationalModel)component.getInputModel()).getModelEntities());
            } else {
                String filterText = filter.toUpperCase();
                for (ModelEntity entity : ((RelationalModel)component.getInputModel()).getModelEntities()) {
                    if (entity.getName().toUpperCase().indexOf(filterText) != -1) {
                        entities.add(entity);
                        continue;
                    }
                    for (ModelAttrib attr : entity.getModelAttributes()) {
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

    class StylesheetChangeListener implements ComponentEventListener<AceValueChanged> {
        public void onComponentEvent(AceValueChanged event) {
            Setting stylesheet = component.findSetting(XsltProcessor.XSLT_PROCESSOR_STYLESHEET);
            stylesheet.setValue(editor.getValue());
            context.getConfigurationService().save(component);
        }
    }

    class TestClickListener implements ComponentEventListener<ClickEvent<Button>> {
        public void onComponentEvent(ClickEvent<Button> event) {
            TestDialog dialog = new TestDialog();
            dialog.show();
        }
    }

    class TestDialog extends ResizableDialog {
        public TestDialog() {
            super("Test Transformation");
            setWidth("800px");
            setHeight("600px");
            innerContent.setMargin(true);
            
            TextArea textField = new TextArea();
            textField.setSizeFull();
            textField.getStyle().set("white-space", "pre").set("overflow-x", "auto").set("padding-bottom", "1em");
            
            Thread thread = Thread.currentThread();
            ClassLoader previousLoader = thread.getContextClassLoader();
            try {
                thread.setContextClassLoader(getClass().getClassLoader());
                textField.setValue(XsltProcessor.getTransformedXml(textArea.getValue(), editor.getValue(), XsltProcessor.PRETTY_FORMAT, false));
            } finally {
                thread.setContextClassLoader(previousLoader);

            }            
            add(textField);
            expand(textField);
            
            buildButtonFooter(buildCloseButton());
        }
    }

}
