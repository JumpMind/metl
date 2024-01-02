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
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.data.HasValue.ValueChangeEvent;
import com.vaadin.data.HasValue.ValueChangeListener;
import com.vaadin.ui.ComboBox;

public class EditTempRdbmsPanel extends AbstractComponentEditPanel {

    private static final long serialVersionUID = 1L;

    AceEditor editor;
    
    ComboBox<String> select;

    @SuppressWarnings("serial")
    protected void buildUI() {
    	ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);
        
    	editor = CommonUiUtils.createAceEditor();
        editor.setMode(AceMode.sql);
        
        select = new ComboBox<String>();
        select.setWidth(40, Unit.EM);
        select.setTextInputAllowed(false);
        
        select.setItems(TempRdbms.DDL, TempRdbms.SQL);
        select.setEmptySelectionAllowed(false);
        select.addValueChangeListener(new ValueChangeListener<String>() {
            @Override
            public void valueChange(ValueChangeEvent<String> event) {
                refresh();
            }
        });
        select.setValue(TempRdbms.SQL);
        buttonBar.addLeft(select);
        
        if (!readOnly) {
            editor.addValueChangeListener(new ValueChangeListener<String>() {

                @Override
                public void valueChange(ValueChangeEvent<String> event) {
                    String key = (String) select.getValue();
                    EditTempRdbmsPanel.this.component.put(key, event.getValue());
                    EditTempRdbmsPanel.this.context.getConfigurationService()
                            .save(EditTempRdbmsPanel.this.component.findSetting(key));
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
