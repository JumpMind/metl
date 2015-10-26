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

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jumpmind.exception.IoException;
import org.jumpmind.metl.core.model.ComponentName;
import org.jumpmind.metl.core.runtime.component.definition.XMLComponent;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.definition.XMLComponentUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.ClassResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.Accordion;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.DragStartMode;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditFlowPalette extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    Accordion componentAccordian;

    ApplicationContext context;

    EditFlowPanel designFlowLayout;

    float splitPosition = 60;

    Unit splitUnit = Unit.PERCENTAGE;

    public EditFlowPalette(EditFlowPanel designFlowLayout, ApplicationContext context, String projectVersionId) {
        this.context = context;
        this.designFlowLayout = designFlowLayout;
        setHeight(100, Unit.PERCENTAGE);
        setWidth(150, Unit.PIXELS);

        setMargin(new MarginInfo(true, false, false, false));

        componentAccordian = new Accordion();
        componentAccordian.setSizeFull();
        addComponent(componentAccordian);
        setExpandRatio(componentAccordian, 1);

        populateComponentPalette(projectVersionId);

    }

    protected String getBase64RepresentationOfImageForComponentType(String type) {
        String resourceName = getImageResourceNameForComponentType(type);
        InputStream is = getClass().getResourceAsStream(resourceName);
        if (is != null) {
            try {
                byte[] bytes = IOUtils.toByteArray(is);
                return new String(Base64.encodeBase64(bytes));
            } catch (IOException e) {
                throw new IoException(e);
            }
        } else {
            return null;
        }
    }

    protected String getImageResourceNameForComponentType(String type) {
        String icon = "/org/jumpmind/metl/core/runtime/component/metl-puzzle-48x48-color.png";
        XMLComponentUI def = context.getUiFactory().getDefinition(type);
        if (def != null && isNotBlank(def.getIconImage())) {
            icon = def.getIconImage();
        }
        return icon;
    }

    protected ClassResource getImageResourceForComponentType(String type) {
        return new ClassResource(getImageResourceNameForComponentType(type));
    }

    protected void populateComponentPalette(String projectVersionId) {
        componentAccordian.removeAllComponents();
        populateComponentTypesInComponentPalette(projectVersionId);
        populateSharedComponentsInComponentPalette(projectVersionId);
    }

    protected void populateComponentTypesInComponentPalette(String projectVersionId) {
        Map<String, List<String>> componentTypesByCategory = context.getComponentFactory().getComponentTypes();
        for (String category : componentTypesByCategory.keySet()) {
            List<String> componentTypes = new ArrayList<String>(componentTypesByCategory.get(category));
            Collections.sort(componentTypes);

            VerticalLayout componentLayout = new VerticalLayout();
            componentAccordian.addTab(componentLayout, StringUtils.isAllUpperCase(category) ? category + "S" : category + "s");
            if (componentTypes != null) {
                for (String componentType : componentTypes) {
                    XMLComponent definition = context.getComponentFactory().getComonentDefinition(componentType);
                    ClassResource icon = getImageResourceForComponentType(componentType);
                    addItemToFlowPanelSection(definition.getName(), componentType, componentLayout, icon, null);
                }
            }
        }
    }

    protected void populateSharedComponentsInComponentPalette(String projectVersionId) {
        VerticalLayout componentLayout = new VerticalLayout();
        componentAccordian.addTab(componentLayout, "SHARED DEFINITIONS");

        List<ComponentName> components = context.getConfigurationService().findSharedComponentsInProject(projectVersionId);
        for (ComponentName component : components) {
            ClassResource icon = getImageResourceForComponentType(component.getType());
            addItemToFlowPanelSection(component.getName(), null, componentLayout, icon, component.getId());
        }
    }

    protected void addItemToFlowPanelSection(String labelName, String componentType, VerticalLayout componentLayout, ClassResource icon, String componentId) {

        FlowPaletteItem paletteItem = new FlowPaletteItem(labelName);
        if (componentId != null) {
            paletteItem.setShared(true);
            paletteItem.setComponentId(componentId);
        } else {
            paletteItem.setComponentType(componentType);
            paletteItem.setShared(false);
        }
        paletteItem.setIcon(icon);
        paletteItem.addStyleName(ValoTheme.BUTTON_ICON_ALIGN_TOP);
        paletteItem.addStyleName(ValoTheme.BUTTON_BORDERLESS_COLORED);
        paletteItem.addStyleName("leftAligned");
        paletteItem.setWidth(100, Unit.PERCENTAGE);
        DragAndDropWrapper wrapper = new DragAndDropWrapper(paletteItem);
        wrapper.setSizeUndefined();
        wrapper.setDragStartMode(DragStartMode.WRAPPER);
        componentLayout.addComponent(wrapper);
        componentLayout.setComponentAlignment(wrapper, Alignment.TOP_CENTER);

    }

}
