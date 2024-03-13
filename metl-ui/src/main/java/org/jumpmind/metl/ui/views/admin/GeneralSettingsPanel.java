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
package org.jumpmind.metl.ui.views.admin;

import org.jumpmind.metl.core.model.GlobalSetting;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.vaadin.flow.component.AbstractField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.converter.StringToIntegerConverter;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.spring.annotation.UIScope;

@SuppressWarnings("serial")
@UiComponent
@UIScope
@Order(500)
@AdminMenuLink(name = "General Settings", id = "General Settings", icon = VaadinIcon.COGS)
public class GeneralSettingsPanel extends AbstractAdminPanel {

    private static final String THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART = "This will take effect on the next server restart";

    final Logger log = LoggerFactory.getLogger(getClass());

    boolean isChanged;

    public GeneralSettingsPanel() {
    }
    
    public void init() {
        FormLayout displaySettingsForm = new FormLayout();
        displaySettingsForm.setResponsiveSteps(new ResponsiveStep("0", 1));

        ((Focusable<?>) addSetting(displaySettingsForm, "System Text", GlobalSetting.SYSTEM_TEXT, "",
                "Set HTML content to be displayed in the top bar that can identify a particular environment"))
                        .focus();
        
        FormLayout autoBackupForm = new FormLayout();
        autoBackupForm.setResponsiveSteps(new ResponsiveStep("0", 1));
        
        Span instructions = new Span("A restart is required after changing these settings");
        instructions.getStyle().set("font-weight", "lighter");
        autoBackupForm.addFormItem(instructions, "");
        
        addSetting(autoBackupForm, "Enable Backup", GlobalSetting.CONFIG_BACKUP_ENABLED,
                Boolean.toString(GlobalSetting.DEFAULT_CONFIG_BACKUP_ENABLED),
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, Boolean.class);

        addSetting(autoBackupForm, "Backup Cron Expression", GlobalSetting.CONFIG_BACKUP_CRON,
                GlobalSetting.DEFAULT_CONFIG_BACKUP_CRON,
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, String.class);

        addSetting(autoBackupForm, "Retention in Days", GlobalSetting.CONFIG_BACKUP_RETENTION_IN_DAYS,
                Integer.toString(GlobalSetting.DEFAULT_CONFIG_BACKUP_RETENTION_IN_DAYS),
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, Integer.class);       

        add(new H3("Display Settings"), displaySettingsForm, new H3("Auto Backup"), autoBackupForm);
    }

    protected AbstractField<?, ?> addSetting(FormLayout form, String text, String globalSetting, String defaultValue,
            String description) {
        return addSetting(form, text, globalSetting, defaultValue, description, String.class);
    }

    protected AbstractField<?, ?> addSetting(FormLayout form, String text, String globalSetting, String defaultValue,
            String description, Class<?> converter) {
        final GlobalSetting setting = getGlobalSetting(globalSetting, defaultValue);
        AbstractField<?, ?> field = null;
        if (Boolean.class.equals(converter)) {
            final Checkbox checkbox = new Checkbox(text);
            checkbox.setValue(Boolean.parseBoolean(setting.getValue()));
            checkbox.addValueChangeListener(
                    (e) -> saveSetting(setting, checkbox.getValue().toString()));
            field = checkbox;
            form.addFormItem(field, "");
        } else {
            field = new TextField();
            ((TextField) field).setWidthFull();
            ((TextField) field).setValueChangeMode(ValueChangeMode.LAZY);
            ((TextField) field).setValueChangeTimeout(200);
            field.addValueChangeListener(event -> saveSetting(setting, (String) event.getValue()));
            field.getElement().setProperty("title", description);
            ((TextField) field).setValue(setting.getValue());

            if (converter.equals(Integer.class)) {
                new Binder<String>().forField((TextField) field)
                        .withConverter(new StringToIntegerConverter("Value must be an integer"))
                        .bind(value -> Integer.parseInt(value), (value, newValue) -> value = String.valueOf(newValue));
            }
            form.addFormItem(field, text);
        }
        return field;
    }

    private void saveSetting(GlobalSetting setting, String value) {
        setting.setValue(value);
        context.getConfigurationService().save(setting);
        isChanged = true;
    }

    @Override
    public boolean closing() {
        if (isChanged) {
        }
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    private GlobalSetting getGlobalSetting(String name, String defaultValue) {
        GlobalSetting setting = context.getOperationsService().findGlobalSetting(name);
        if (setting == null) {
            setting = new GlobalSetting();
            setting.setName(name);
            setting.setValue(defaultValue);
        }
        return setting;
    }

    @Override
    protected void refresh() {
        // TODO Auto-generated method stub
        
    }

}
