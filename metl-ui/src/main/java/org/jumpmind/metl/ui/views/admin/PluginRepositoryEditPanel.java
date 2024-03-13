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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.apache.commons.lang.StringUtils;
import org.jumpmind.metl.core.model.PluginRepository;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.IUiPanel;

import com.vaadin.flow.component.HasValue.ValueChangeEvent;
import com.vaadin.flow.component.HasValue.ValueChangeListener;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.formlayout.FormLayout.ResponsiveStep;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.Command;

@SuppressWarnings("serial")

public class PluginRepositoryEditPanel extends VerticalLayout implements IUiPanel {

    protected static final String NOCHANGE = "******";

    ApplicationContext context;

    PluginRepository pluginRepository;
    
    Command refreshGridCommand;

    public PluginRepositoryEditPanel(ApplicationContext context, PluginRepository pluginRepository, Command refreshGridCommand) {
        this.context = context;
        this.pluginRepository = pluginRepository;
        this.refreshGridCommand = refreshGridCommand;

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new ResponsiveStep("0", 1));

        TextField field = new TextField();
        field.setValue(StringUtils.trimToEmpty(pluginRepository.getName()));
        field.setWidth("20em");
        form.addFormItem(field, "Name");
        field.addValueChangeListener(new NameChangeListener());
        field.focus();

        field = new TextField();
        field.setValue(StringUtils.trimToEmpty(pluginRepository.getUrl()));
        field.setWidth("45em");
        field.addValueChangeListener(new UrlChangeListener());
        form.addFormItem(field, "Url");

        add(form);
    }

    @Override
    public boolean closing() {
        refreshGridCommand.execute();
        return true;
    }

    @Override
    public void deselected() {
    }

    @Override
    public void selected() {
    }

    protected void save(PluginRepository pluginRepository) {
        if (isNotBlank(pluginRepository.getName())) {
            context.getConfigurationService().save(pluginRepository);
        }
    }

    class UrlChangeListener implements ValueChangeListener<ValueChangeEvent<String>> {
        public void valueChanged(ValueChangeEvent<String> event) {
            pluginRepository.setUrl(event.getValue());
            save(pluginRepository);
        }
    }

    class NameChangeListener implements ValueChangeListener<ValueChangeEvent<String>> {
        public void valueChanged(ValueChangeEvent<String> event) {
            pluginRepository.setName(event.getValue());
            save(pluginRepository);
        }
    }   

}
