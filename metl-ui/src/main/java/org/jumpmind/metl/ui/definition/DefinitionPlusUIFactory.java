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
package org.jumpmind.metl.ui.definition;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.IOUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IPluginService;
import org.jumpmind.metl.core.plugin.DefinitionFactory;
import org.jumpmind.metl.core.plugin.IPluginManager;
import org.jumpmind.metl.ui.views.design.IComponentEditPanel;

public class DefinitionPlusUIFactory extends DefinitionFactory implements IDefinitionPlusUIFactory {

    protected Map<String, Map<String, XMLComponentUI>> uisByProjectVersionIdByComponentId;

    protected JAXBContext xmlUiContext;

    public DefinitionPlusUIFactory(IPluginService pluginService, IConfigurationService configurationService, IPluginManager pluginManager) {
        super(pluginService, configurationService, pluginManager);
    }

    @Override
    public void refresh() {
        uisByProjectVersionIdByComponentId = new HashMap<>();
        super.refresh();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void loadComponentsForClassloader(String projectVersionId, String pluginId, ClassLoader classLoader) {
        super.loadComponentsForClassloader(projectVersionId, pluginId, classLoader);
        Map<String, XMLComponentUI> componentsById = uisByProjectVersionIdByComponentId.get(projectVersionId);
        if (componentsById == null) {
            componentsById = new HashMap<>();
            uisByProjectVersionIdByComponentId.put(projectVersionId, componentsById);
        }
        try {
            if (xmlUiContext == null) {
                synchronized (this) {
                    if (xmlUiContext == null) {
                        xmlUiContext = JAXBContext.newInstance(XMLUI.class, XMLComponentUI.class, ObjectFactory.class);
                    }
                }
            }
            Unmarshaller unmarshaller = xmlUiContext.createUnmarshaller();

            List<InputStream> componentXmls = loadResources("ui.xml", classLoader);
            try {
                for (InputStream inputStream : componentXmls) {
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    JAXBElement<XMLUI> root = (JAXBElement<XMLUI>) unmarshaller.unmarshal(reader);
                    XMLUI ui = root.getValue();
                    List<XMLComponentUI> componentUis = ui.getComponentUis();
                    for (XMLComponentUI xmlComponentUI : componentUis) {
                        if (!componentsById.containsKey(xmlComponentUI.getComponentId())) {
                            xmlComponentUI.setClassLoader(classLoader);
                            componentsById.put(xmlComponentUI.getComponentId(), xmlComponentUI);
                        }
                    }
                }
            } finally {
                for (InputStream inputStream : componentXmls) {
                    IOUtils.closeQuietly(inputStream);
                }

            }
        } catch (Exception e) {
            throw new IoException(e);
        }
    }

    @Override
    public IComponentEditPanel createUiPanel(String projectVersionId, String componentId) {
        try {
            XMLComponentUI ui = getUiDefinition(projectVersionId, componentId);
            if (ui != null && isNotBlank(ui.getClassName())) {
                return (IComponentEditPanel) Class.forName(ui.getClassName(), true, ui.getClassLoader()).newInstance();
            } else {
                return null;
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public XMLComponentUI getUiDefinition(String projectVersionId, String componentId) {
        Map<String, XMLComponentUI> componentsById = uisByProjectVersionIdByComponentId.get(projectVersionId);
        if (componentsById != null) {
            return componentsById.get(componentId);
        } else {
            logger.warn("Could not find components for project version of {}", projectVersionId);
            return null;
        }
    }

}
