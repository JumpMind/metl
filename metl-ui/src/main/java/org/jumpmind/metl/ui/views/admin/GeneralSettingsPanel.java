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
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;

import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.ui.AbstractField;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.Label;
import com.vaadin.v7.ui.VerticalLayout;

@SuppressWarnings("serial")
@UiComponent
@UIScope
@Order(500)
@AdminMenuLink(name = "General Settings", id = "General Settings", icon = FontAwesome.GEARS)
public class GeneralSettingsPanel extends AbstractAdminPanel {

    private static final String THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART = "This will take effect on the next server restart";

    final Logger log = LoggerFactory.getLogger(getClass());

    boolean isChanged;

    FormLayout form;

    public GeneralSettingsPanel() {
    }
    
    public void init() {
        form = new FormLayout();
        form.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);

        Label section = new Label("Display Settings");
        section.addStyleName(ValoTheme.LABEL_H3);
        section.addStyleName(ValoTheme.LABEL_COLORED);
        form.addComponent(section);

        addSetting("System Text", GlobalSetting.SYSTEM_TEXT, "",
                "Set HTML content to be displayed in the top bar that can identify a particular environment")
                        .focus();
        
        section = new Label("Auto Backup");
        section.addStyleName(ValoTheme.LABEL_H3);
        section.addStyleName(ValoTheme.LABEL_COLORED);
        form.addComponent(section); 
        
        Label instructions = new Label("A restart is required after changing these settings");
        instructions.addStyleName(ValoTheme.LABEL_LIGHT);
        form.addComponent(instructions);
        
        addSetting("Enable Backup", GlobalSetting.CONFIG_BACKUP_ENABLED,
                Boolean.toString(GlobalSetting.DEFAULT_CONFIG_BACKUP_ENABLED),
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, Boolean.class);

        addSetting("Backup Cron Expression", GlobalSetting.CONFIG_BACKUP_CRON,
                GlobalSetting.DEFAULT_CONFIG_BACKUP_CRON,
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, String.class);

        addSetting("Retention in Days", GlobalSetting.CONFIG_BACKUP_RETENTION_IN_DAYS,
                Integer.toString(GlobalSetting.DEFAULT_CONFIG_BACKUP_RETENTION_IN_DAYS),
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, Integer.class);       

        VerticalLayout paddedLayout = new VerticalLayout();
        paddedLayout.setMargin(true);
        paddedLayout.addComponent(form);
        addComponent(paddedLayout);
    }

    protected AbstractField<?> addSetting(String text, String globalSetting, String defaultValue,
            String description) {
        return addSetting(text, globalSetting, defaultValue, description, String.class);
    }

    protected AbstractField<?> addSetting(String text, String globalSetting, String defaultValue,
            String description, Class<?> converter) {
        final GlobalSetting setting = getGlobalSetting(globalSetting, defaultValue);
        AbstractField<?> field = null;
        if (Boolean.class.equals(converter)) {
            final CheckBox checkbox = new CheckBox(text);
            checkbox.setImmediate(true);
            checkbox.setValue(Boolean.parseBoolean(setting.getValue()));
            checkbox.addValueChangeListener(
                    (e) -> saveSetting(setting, checkbox.getValue().toString()));
            field = checkbox;
        } else {
            field = new ImmediateUpdateTextField(text) {
                protected void save(String value) {
                    saveSetting(setting, value);
                }
            };
            field.setDescription(description);
            ((ImmediateUpdateTextField) field).setValue(setting.getValue());

            if (converter != null) {
                field.setConverter(converter);
            }
        }
        form.addComponent(field);
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
    public void enter(ViewChangeEvent event) {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void refresh() {
        // TODO Auto-generated method stub
        
    }

}
