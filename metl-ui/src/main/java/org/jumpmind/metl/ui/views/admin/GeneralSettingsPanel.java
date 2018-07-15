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
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.TabbedPanel;
import org.jumpmind.metl.ui.i18n.MessageSource;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.AbstractField;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class GeneralSettingsPanel extends Panel implements IUiPanel {

    private static final String THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART = "This will take effect on the next server restart";

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    TabbedPanel tabbedPanel;

    boolean isChanged;

    FormLayout form;

    public GeneralSettingsPanel(final ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;

        form = new FormLayout();
        form.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
     
        Label section = new Label(MessageSource.message("generalSettingsPanel.display.settings"));
        section.addStyleName(ValoTheme.LABEL_H3);
        section.addStyleName(ValoTheme.LABEL_COLORED);
        form.addComponent(section);

        addSetting(MessageSource.message("generalSettingsPanel.system.text"), GlobalSetting.SYSTEM_TEXT, "",
        		MessageSource.message("generalSettingsPanel.intro"))
                        .focus();

        section = new Label(MessageSource.message("generalSettingsPanel.purge.settings"));
        section.addStyleName(ValoTheme.LABEL_H3);
        section.addStyleName(ValoTheme.LABEL_COLORED);
        form.addComponent(section);
       
        addSetting(MessageSource.message("generalSettingsPanel.audit.event.retention.in.days"), GlobalSetting.AUDIT_EVENT_RETENTION_IN_DAYS,
                Integer.toString(GlobalSetting.DEFAULT_AUDIT_EVENT_RETENTION_IN_DAYS), "",
                Integer.class);
        
        section = new Label(MessageSource.message("generalSettingsPanel.auto.backup"));
        section.addStyleName(ValoTheme.LABEL_H3);
        section.addStyleName(ValoTheme.LABEL_COLORED);
        form.addComponent(section); 
        
        Label instructions = new Label(MessageSource.message("generalSettingsPanel.a.restart.is.required.after.changing.these.settings"));
        instructions.addStyleName(ValoTheme.LABEL_LIGHT);
        form.addComponent(instructions);
       
        addSetting(MessageSource.message("generalSettingsPanel.enable.backup"), GlobalSetting.CONFIG_BACKUP_ENABLED,
                Boolean.toString(GlobalSetting.DEFAULT_CONFIG_BACKUP_ENABLED),
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, Boolean.class);

        addSetting(MessageSource.message("generalSettingsPanel.backup.cron.expression") , GlobalSetting.CONFIG_BACKUP_CRON,
                GlobalSetting.DEFAULT_CONFIG_BACKUP_CRON,
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, String.class);
       
        addSetting(MessageSource.message("generalSettingsPanel.retention.in.days"), GlobalSetting.CONFIG_BACKUP_RETENTION_IN_DAYS,
                Integer.toString(GlobalSetting.DEFAULT_CONFIG_BACKUP_RETENTION_IN_DAYS),
                THIS_WILL_TAKE_EFFECT_ON_THE_NEXT_SERVER_RESTART, Integer.class);       
        
        section = new Label(MessageSource.message("generalSettingsPanel.user.password.settings"));
        section.addStyleName(ValoTheme.LABEL_H3);
        section.addStyleName(ValoTheme.LABEL_COLORED);
        form.addComponent(section);

        addSetting(MessageSource.message("generalSettingsPanel.minimum.length"), GlobalSetting.PASSWORD_MIN_LENGTH, "6", "", Integer.class);
    
        addSetting(MessageSource.message("generalSettingsPanel.prohibit.reuse") , GlobalSetting.PASSWORD_PROHIBIT_PREVIOUS, "5", "",
                Integer.class);

        addSetting(MessageSource.message("generalSettingsPanel.expiration.in.days") , GlobalSetting.PASSWORD_EXPIRE_DAYS, "60", "",
                Integer.class);
        
        addSetting(MessageSource.message("generalSettingsPanel.number.of.failed.attempts") , GlobalSetting.PASSWORD_FAILED_ATTEMPTS, 
        		Integer.toString(GlobalSetting.PASSWORD_FAILED_ATTEMPTS_DEFAULT), "", Integer.class);

        addSetting(MessageSource.message("generalSettingsPanel.prohibit.common.words") , GlobalSetting.PASSWORD_PROHIBIT_COMMON_WORDS, "true",
                "", Boolean.class);
        // MessageSource.message("generalSettingsPanel.require.mixed.case") 
        addSetting(MessageSource.message("generalSettingsPanel.require.alphanumeric") , GlobalSetting.PASSWORD_REQUIRE_ALPHANUMERIC, "true", "",
                Boolean.class);

        addSetting(MessageSource.message("generalSettingsPanel.require.symbol") , GlobalSetting.PASSWORD_REQUIRE_SYMBOL, "true", "",
                Boolean.class);

        addSetting(MessageSource.message("generalSettingsPanel.require.mixed.case"), GlobalSetting.PASSWORD_REQUIRE_MIXED_CASE, "true", "",
                Boolean.class);

        VerticalLayout paddedLayout = new VerticalLayout();
        paddedLayout.setMargin(true);
        paddedLayout.addComponent(form);
        setContent(paddedLayout);
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

}
