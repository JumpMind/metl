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

import org.jumpmind.metl.core.runtime.component.TempRdbms;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;

import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.combobox.ComboBox;

import de.f0rce.ace.AceEditor;
import de.f0rce.ace.enums.AceMode;
import de.f0rce.ace.events.AceValueChanged;

public class EditTempRdbmsPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;

    AceEditor editor;
    
    ComboBox<String> select;

    @SuppressWarnings("serial")
    protected void buildUI() {
    	ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);
        
    	editor = CommonUiUtils.createAceEditor();
        editor.setMode(AceMode.sql);
        
        select = new ComboBox<String>();
        select.setWidth("40em");
        select.setAllowCustomValue(false);
        
        select.setItems(TempRdbms.DDL, TempRdbms.SQL);
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
        select.setValue(TempRdbms.SQL);
        buttonBar.addLeft(select);
        
        if (!readOnly) {
            editor.addValueChangeListener(new ComponentEventListener<AceValueChanged>() {

                @Override
                public void onComponentEvent(AceValueChanged event) {
                    String key = (String) select.getValue();
                    EditTempRdbmsPanel.this.component.put(key, event.getValue());
                    EditTempRdbmsPanel.this.context.getConfigurationService()
                            .save(EditTempRdbmsPanel.this.component.findSetting(key));
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
