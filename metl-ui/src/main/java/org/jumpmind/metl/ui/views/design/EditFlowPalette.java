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

import com.vaadin.flow.component.dnd.DragSource;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.InputStreamFactory;
import com.vaadin.flow.server.StreamResource;

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
        
        setHeightFull();
        setWidth("190px");
        setPadding(false);
        setSpacing(false);

        HorizontalLayout topWrapper = new HorizontalLayout();
        HorizontalLayout top = new HorizontalLayout();
        top.addClassName(ButtonBar.STYLE);
        top.setMargin(true);
        topWrapper.add(top);
        
        add(topWrapper);
        
        TextField filterField = new TextField();
        filterField.setWidth("160px");
        filterField.setPlaceholder("Filter");
        filterField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        filterField.setValueChangeMode(ValueChangeMode.LAZY);
        filterField.setValueChangeTimeout(200);
        filterField.addValueChangeListener((event) -> {
            filterText = event.getValue().toLowerCase();
            populateComponentPalette();
        });
        top.add(filterField);

        Scroller panel = new Scroller();
        panel.setSizeFull();

        componentLayout = new VerticalLayout();
        componentLayout.setPadding(false);
        componentLayout.getStyle().set("margin", "16px 0 0 0");
        componentLayout.addClassName("scrollable");
        panel.setContent(componentLayout);

        addAndExpand(panel);

        populateComponentPalette();

    }  

    protected StreamResource getImageResourceForComponentType(String projectVersionId, XMLComponentDefinition componentDefinition) {
        InputStreamFactory factory = new InputStreamFactory() {
            private static final long serialVersionUID = 1L;

            @Override
            public InputStream createInputStream() {
                return UiUtils.getComponentImageInputStream(projectVersionId, componentDefinition.getId(), context);
            }
        };
        return new StreamResource(componentDefinition.getId(), factory);
    }
    protected void populateComponentPalette() {
        componentLayout.removeAll();
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

        FlowPaletteItem paletteItem = new FlowPaletteItem(labelName, icon);
        if (componentId != null) {
            paletteItem.setShared(true);
            paletteItem.setComponentId(componentId);
        } else {
            paletteItem.setComponentType(componentType);
            paletteItem.setShared(false);
        }
        paletteItem.addClassName("leftAligned");
        DragSource<FlowPaletteItem> extension = DragSource.create(paletteItem);
        extension.setDragData(paletteItem);
        componentLayout.add(paletteItem);
        componentLayout.setHorizontalComponentAlignment(Alignment.CENTER, paletteItem);

    }

}
