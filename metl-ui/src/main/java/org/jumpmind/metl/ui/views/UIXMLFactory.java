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
package org.jumpmind.metl.ui.views;

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
import org.jumpmind.metl.core.util.AbstractXMLFactory;
import org.jumpmind.metl.ui.definition.XMLComponentUI;
import org.jumpmind.metl.ui.definition.XMLUI;
import org.jumpmind.metl.ui.views.design.IComponentEditPanel;

public class UIXMLFactory extends AbstractXMLFactory implements IUIFactory {

    protected Map<String, XMLComponentUI> componentUisByComponentId;
    
    public UIXMLFactory() {
        super(null);
    }
    
    synchronized public XMLComponentUI getDefinition(String componentId) {
        return componentUisByComponentId.get(componentId);
    }

    public IComponentEditPanel create(String componentId) {
        try {
            XMLComponentUI ui = getDefinition(componentId);
            if (ui != null && isNotBlank(ui.getClassName())) {
                return (IComponentEditPanel) Class.forName(ui.getClassName()).newInstance();
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
    protected void reset() {
        componentUisByComponentId = new HashMap<String, XMLComponentUI>();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void loadComponentsForClassloader(ClassLoader classLoader) {
        try {
            JAXBContext jc = JAXBContext.newInstance(XMLUI.class.getPackage().getName());
            Unmarshaller unmarshaller = jc.createUnmarshaller();

            List<InputStream> componentXmls = loadResources("ui.xml", classLoader);
            try {
                for (InputStream inputStream : componentXmls) {
                    InputStreamReader reader = new InputStreamReader(inputStream);
                    JAXBElement<XMLUI> root = (JAXBElement<XMLUI>) unmarshaller.unmarshal(reader);
                    XMLUI ui = root.getValue();
                    List<XMLComponentUI> componentUis = ui.getComponentUis();
                    for (XMLComponentUI xmlComponentUI : componentUis) {
                        componentUisByComponentId.put(xmlComponentUI.getComponentId(), xmlComponentUI);
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

}
