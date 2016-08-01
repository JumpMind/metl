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

import com.vaadin.data.Property.ValueChangeEvent;
import com.vaadin.data.Property.ValueChangeListener;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")

public class PluginRepositoryEditPanel extends VerticalLayout implements IUiPanel {

    protected static final String NOCHANGE = "******";

    ApplicationContext context;

    PluginRepository pluginRepository;

    public PluginRepositoryEditPanel(ApplicationContext context, PluginRepository pluginRepository) {
        this.context = context;
        this.pluginRepository = pluginRepository;

        FormLayout form = new FormLayout();
        form.setSpacing(true);

        TextField field = new TextField("Name", StringUtils.trimToEmpty(pluginRepository.getName()));
        field.setWidth(20, Unit.EM);
        form.addComponent(field);
        field.addValueChangeListener(new NameChangeListener());
        field.focus();

        field = new TextField("Url", StringUtils.trimToEmpty(pluginRepository.getUrl()));
        field.setWidth(45, Unit.EM);
        field.addValueChangeListener(new UrlChangeListener());
        form.addComponent(field);

        addComponent(form);
        setMargin(true);
    }

    @Override
    public boolean closing() {
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

    class UrlChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            pluginRepository.setUrl((String) event.getProperty().getValue());
            save(pluginRepository);
        }
    }

    class NameChangeListener implements ValueChangeListener {
        public void valueChange(ValueChangeEvent event) {
            pluginRepository.setName((String) event.getProperty().getValue());
            save(pluginRepository);
        }
    }   

}
