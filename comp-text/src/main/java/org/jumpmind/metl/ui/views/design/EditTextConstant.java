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

import org.jumpmind.metl.core.model.Setting;
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
