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

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;

public class EditScriptPanel extends AbstractComponentEditPanel {

    protected static final String SCRIPT_ON_ERROR = "onError(myError, allStepErrors)";
    protected static final String SCRIPT_ON_SUCCESS = "onSuccess()";
    protected static final String SCRIPT_ON_INIT = "onInit()";
    protected static final String SCRIPT_ON_HANDLE = "onHandleMessage(inputMessage, messageTarget)";
    protected static final String SCRIPT_IMPORTS = "<Imports>";
    protected static final String SCRIPT_METHODS = "<Methods>";

    private static final long serialVersionUID = 1L;

    AceEditor editor;

    ComboBox<String> select;

    @SuppressWarnings("serial")
    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);

        buttonBar.addButtonRight("Templates", VaadinIcon.QUESTION_CIRCLE,
                (e) -> new ScriptTemplatesDialog(this, context, component, readOnly)
                        .showAtSize(.75));

        editor = CommonUiUtils.createAceEditor();

        editor.setMode(AceMode.java);

        select = new ComboBox<String>();
        select.setWidth("40em");
        select.setAllowCustomValue(false);

		select.setItems(Script.IMPORTS, Script.METHODS, Script.INIT_SCRIPT, Script.HANDLE_SCRIPT,
				Script.ON_FLOW_SUCCESS, Script.ON_FLOW_ERROR);
		select.setItemLabelGenerator(item -> {
			switch (item) {
				case Script.IMPORTS:
					return SCRIPT_IMPORTS;
				case Script.METHODS:
					return SCRIPT_METHODS;
				case Script.INIT_SCRIPT:
					return SCRIPT_ON_INIT;
				case Script.HANDLE_SCRIPT:
					return SCRIPT_ON_HANDLE;
				case Script.ON_FLOW_SUCCESS:
					return SCRIPT_ON_SUCCESS;
				case Script.ON_FLOW_ERROR:
					return SCRIPT_ON_ERROR;
				default:
					return "";
			}
		});

        select.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {
            @Override
            public void valueChanged(ValueChangeEvent<String> event) {
                if (event.getValue() != null) {
                    refresh();
                } else {
                    select.setValue(event.getOldValue());
                }
            }
        });
        select.setValue(Script.HANDLE_SCRIPT);
        buttonBar.addLeft(select);

        if (!readOnly) {
            editor.addValueChangeListener(new ValueChangeListener<ValueChangeEvent<String>>() {

                @Override
                public void valueChanged(ValueChangeEvent<String> event) {
                    String key = (String) select.getValue();
                    EditScriptPanel.this.component.put(key, event.getValue());
                    EditScriptPanel.this.context.getConfigurationService()
                            .save(EditScriptPanel.this.component.findSetting(key));
                }
            });
        }

        add(editor);
        expand(editor);

    }

    protected void refresh() {
        String key = (String) select.getValue();
        editor.setReadOnly(false);
        editor.setValue(
                component.get(key, componentDefinition.findXMLSetting(key).getDefaultValue()));
        editor.setReadOnly(readOnly);
    }

}
