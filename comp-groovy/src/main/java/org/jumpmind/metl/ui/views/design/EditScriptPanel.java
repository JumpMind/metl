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

import org.jumpmind.metl.core.runtime.component.Script;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.ComboBox;

public class EditScriptPanel extends AbstractComponentEditPanel {

    protected static final String SCRIPT_ON_ERROR = "onError(myError, allStepErrors)";
    protected static final String SCRIPT_ON_SUCCESS = "onSuccess()";
    protected static final String SCRIPT_ON_INIT = "onInit()";
    protected static final String SCRIPT_ON_HANDLE = "onHandleMessage(inputMessage, messageTarget)";
    protected static final String SCRIPT_IMPORTS = "<Imports>";
    protected static final String SCRIPT_METHODS = "<Methods>";

    private static final long serialVersionUID = 1L;

    AceEditor editor;

    ComboBox select;

    @SuppressWarnings("serial")
    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        buttonBar.addButtonRight("Templates", FontAwesome.QUESTION_CIRCLE,
                (e) -> new ScriptTemplatesDialog(this, context, component, readOnly)
                        .showAtSize(.75));

        editor = CommonUiUtils.createAceEditor();
        editor.setTextChangeEventMode(TextChangeEventMode.LAZY);
        editor.setTextChangeTimeout(200);

        editor.setMode(AceMode.java);

        select = new ComboBox();
        select.setWidth(40, Unit.EM);
        select.setTextInputAllowed(false);

        select.addItem(Script.IMPORTS);
        select.setItemCaption(Script.IMPORTS, SCRIPT_IMPORTS);
        select.addItem(Script.METHODS);
        select.setItemCaption(Script.METHODS, SCRIPT_METHODS);
        select.addItem(Script.INIT_SCRIPT);
        select.setItemCaption(Script.INIT_SCRIPT, SCRIPT_ON_INIT);
        select.addItem(Script.HANDLE_SCRIPT);
        select.setItemCaption(Script.HANDLE_SCRIPT, SCRIPT_ON_HANDLE);
        select.addItem(Script.ON_FLOW_SUCCESS);
        select.setItemCaption(Script.ON_FLOW_SUCCESS, SCRIPT_ON_SUCCESS);
        select.addItem(Script.ON_FLOW_ERROR);
        select.setItemCaption(Script.ON_FLOW_ERROR, SCRIPT_ON_ERROR);

        select.setImmediate(true);
        select.setNullSelectionAllowed(false);
        select.setNewItemsAllowed(false);
        select.addValueChangeListener(new ValueChangeListener() {
            @Override
            public void valueChange(ValueChangeEvent event) {
                refresh();
            }
        });
        select.setValue(Script.HANDLE_SCRIPT);
        buttonBar.addLeft(select);

        if (!readOnly) {
            editor.addTextChangeListener(new TextChangeListener() {

                @Override
                public void textChange(TextChangeEvent event) {
                    String key = (String) select.getValue();
                    EditScriptPanel.this.component.put(key, event.getText());
                    EditScriptPanel.this.context.getConfigurationService()
                            .save(EditScriptPanel.this.component.findSetting(key));
                }
            });
        }

        addComponent(editor);
        setExpandRatio(editor, 1);

    }

    protected void refresh() {
        String key = (String) select.getValue();
        editor.setReadOnly(false);
        editor.setValue(
                component.get(key, componentDefinition.findXMLSetting(key).getDefaultValue()));
        editor.setReadOnly(readOnly);
    }

}
