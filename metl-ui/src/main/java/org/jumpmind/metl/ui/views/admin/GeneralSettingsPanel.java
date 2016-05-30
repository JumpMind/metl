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
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.jumpmind.vaadin.ui.common.ImmediateUpdateTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.FormLayout;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class GeneralSettingsPanel extends VerticalLayout implements IUiPanel {

    final Logger log = LoggerFactory.getLogger(getClass());
    
    ApplicationContext context;

    TabbedPanel tabbedPanel;
    
    boolean isChanged;

    public GeneralSettingsPanel(final ApplicationContext context, TabbedPanel tabbedPanel) {
        this.context = context;
        this.tabbedPanel = tabbedPanel;

        final GlobalSetting systemTextSetting = getGlobalSetting(GlobalSetting.SYSTEM_TEXT, "");

        FormLayout form = new FormLayout();
        form.setSpacing(true);

        ImmediateUpdateTextField systemTextField = new ImmediateUpdateTextField("System Text") {
            protected void save(String value) {
                saveSetting(systemTextSetting, value);
            }
        };
        systemTextField.setDescription("Set HTML content to be displayed in the top bar that can identify a particular environment");
        systemTextField.setValue(systemTextSetting.getValue());
        systemTextField.setWidth(25f, Unit.EM);
        form.addComponent(systemTextField);
        systemTextField.focus();

        addComponent(form);
        setMargin(true);
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
        GlobalSetting setting = context.getConfigurationService().findGlobalSetting(name);
        if (setting == null) {
            setting = new GlobalSetting();
            setting.setName(name);
            setting.setValue(defaultValue);
        }
        return setting;
    }

}
