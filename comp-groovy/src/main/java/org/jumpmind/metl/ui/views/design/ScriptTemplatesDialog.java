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

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.runtime.component.Script;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.vaadin.aceeditor.AceEditor;
import org.vaadin.aceeditor.AceMode;

import com.vaadin.ui.Button;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.themes.ValoTheme;

class ScriptTemplatesDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;
    
    final Logger logger = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    Component component;

    AceEditor editor;

    EditScriptPanel parent;

    public ScriptTemplatesDialog(EditScriptPanel parent, ApplicationContext context,
            Component component, boolean readOnly) {
        super("Script Templates");

        this.parent = parent;
        this.context = context;
        this.component = component;

        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);

        ComboBox templates = new ComboBox();
        templates.setWidth(400, Unit.PIXELS);
        templates.setNullSelectionAllowed(false);
        buttonBar.addLeft(templates);

        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(
                    ScriptTemplatesDialog.class.getClassLoader());
            Resource[] resources = resolver
                    .getResources("classpath:/org/jumpmind/metl/ui/examples/scripts/*.groovy");
            for (Resource resource : resources) {
                templates.addItem(new Template(resource.getFilename()));
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.error("", e);
        }

        templates.addValueChangeListener(
                (e) -> editor.setValue(((Template) e.getProperty().getValue()).script));

        editor = CommonUiUtils.createAceEditor();
        editor.setSizeFull();
        editor.setMode(AceMode.java);
        addComponent(editor, 1);

        templates.setValue(templates.getItemIds().iterator().next());

        Button applyButton = new Button("Apply This Template",
                e -> notifyApplyTemplate((Template) templates.getValue()));

        Button closeButton = new Button("Close");
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        closeButton.addClickListener((e) -> close());

        addComponent(buildButtonFooter(applyButton, closeButton));

    }

    protected void notifyApplyTemplate(Template template) {
        ConfirmDialog.show("Apply the '" + template.name + "' template?",
                "Are you sure you want to apply the '" + template.name + "' template?", () -> {
                    applyTemplate(template);
                    return true;
                });
    }

    protected void applyTemplate(Template template) {
        String script = template.script;
        applyTemplate(script, Script.HANDLE_SCRIPT, EditScriptPanel.SCRIPT_ON_HANDLE);
        applyTemplate(script, Script.INIT_SCRIPT, EditScriptPanel.SCRIPT_ON_INIT);
        applyTemplate(script, Script.IMPORTS, EditScriptPanel.SCRIPT_IMPORTS);
        applyTemplate(script, Script.METHODS, EditScriptPanel.SCRIPT_METHODS);
        applyTemplate(script, Script.ON_FLOW_SUCCESS, EditScriptPanel.SCRIPT_ON_SUCCESS);
        applyTemplate(script, Script.ON_FLOW_ERROR, EditScriptPanel.SCRIPT_ON_ERROR);
        close();
    }

    protected void applyTemplate(String fullScript, String settingKey, String methodName) {
        final String START = "// " + methodName;
        final String END = "// end";
        int startIndex = fullScript.indexOf(START);
        String text = null;
        if (startIndex >= 0) {
            text = fullScript.substring(startIndex + START.length());
            int endIndex = text.indexOf(END, startIndex);
            if (endIndex >= 0) {
                text = text.substring(0, endIndex);
            }
        }
        this.component.put(settingKey, text);
        this.context.getConfigurationService().save(this.component.findSetting(settingKey));
        this.parent.refresh();
    }

    class Template {

        String script;
        String name;

        public Template(String fileName) {
            name = fileName.replaceAll("\\-", " ");
            name = name.substring(0, fileName.length() - ".groovy".length());
            try {
                script = IOUtils.toString(getClass()
                        .getResourceAsStream("/org/jumpmind/metl/ui/examples/scripts/" + fileName));
            } catch (IOException e) {
                script = "Failed to read " + fileName;
            }
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
