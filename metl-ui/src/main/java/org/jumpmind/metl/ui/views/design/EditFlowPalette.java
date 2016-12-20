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

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.InputStream;
import java.util.Collections;
import java.util.List;

import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.UiUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.server.FontAwesome;
import com.vaadin.server.StreamResource;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.DragAndDropWrapper;
import com.vaadin.ui.DragAndDropWrapper.DragStartMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class EditFlowPalette extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    EditFlowPanel designFlowLayout;

    VerticalLayout componentLayout;

    String filterText;

    String projectVersionId;
    
    public EditFlowPalette(EditFlowPanel designFlowLayout, ApplicationContext context, String projectVersionId) {
        this.context = context;
        this.designFlowLayout = designFlowLayout;
        this.projectVersionId = projectVersionId;

        final int WIDTH = 150;
        
        setHeight(100, Unit.PERCENTAGE);
        setWidth(WIDTH, Unit.PIXELS);

        HorizontalLayout topWrapper = new HorizontalLayout();
        topWrapper.setMargin(new MarginInfo(true, false, false, false));
        HorizontalLayout top = new HorizontalLayout();
        top.addStyleName(ButtonBar.STYLE);
        top.setMargin(true);
        topWrapper.addComponent(top);
        
        addComponent(topWrapper);
        
        TextField filterField = new TextField();
        filterField.setWidth(WIDTH-30, Unit.PIXELS);
        filterField.setInputPrompt("Filter");
        filterField.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        filterField.setIcon(FontAwesome.SEARCH);
        filterField.setImmediate(true);
        filterField.setTextChangeEventMode(TextChangeEventMode.LAZY);
        filterField.setTextChangeTimeout(200);
        filterField.addTextChangeListener((event) -> {
            filterText = event.getText().toLowerCase();
            populateComponentPalette();
        });
        top.addComponent(filterField);

        Panel panel = new Panel();
        panel.setSizeFull();
        panel.addStyleName(ValoTheme.PANEL_BORDERLESS);
        panel.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);

        componentLayout = new VerticalLayout();
        componentLayout.setMargin(new MarginInfo(true, false, false, false));
        componentLayout.addStyleName("scrollable");
        panel.setContent(componentLayout);

        addComponent(panel);
        setExpandRatio(panel, 1);

        populateComponentPalette();

    }  

    protected StreamResource getImageResourceForComponentType(String projectVersionId, XMLComponentDefinition componentDefinition) {
        StreamResource.StreamSource source = new StreamResource.StreamSource() {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream getStream() {
                return UiUtils.getComponentImageInputStream(projectVersionId, componentDefinition.getId(), context);
            }
        };
        return new StreamResource(source, componentDefinition.getId());
    }
    protected void populateComponentPalette() {
        componentLayout.removeAllComponents();
        List<XMLComponentDefinition> componentDefinitions = context.getDefinitionFactory().getComponentDefinitions(projectVersionId);
        Collections.sort(componentDefinitions);
        for (XMLComponentDefinition definition : componentDefinitions) {
            if ((isBlank(filterText) || definition.getName().toLowerCase().contains(filterText)
                    || definition.getCategory().toLowerCase().contains(filterText) || definition.getKeywords().toLowerCase().contains(filterText))) {
                StreamResource icon = getImageResourceForComponentType(projectVersionId, definition);
                addItemToFlowPanelSection(definition.getName(), definition.getId(), componentLayout, icon, null);
            }
        }
    }

    protected void addItemToFlowPanelSection(String labelName, String componentType, VerticalLayout componentLayout, StreamResource icon,
            String componentId) {

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
