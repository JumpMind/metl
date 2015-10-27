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
package org.jumpmind.metl.ui.mapping;

import org.jumpmind.metl.core.model.ComponentAttributeSetting;
import org.jumpmind.metl.core.model.ModelAttribute;
import org.jumpmind.metl.core.model.ModelEntity;
import org.jumpmind.metl.core.runtime.component.Mapping;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.views.design.AbstractComponentEditPanel;

import com.vaadin.event.FieldEvents.TextChangeEvent;
import com.vaadin.event.FieldEvents.TextChangeListener;
import com.vaadin.server.FontAwesome;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.AbstractTextField.TextChangeEventMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@SuppressWarnings("serial")
public class EditMappingPanel extends AbstractComponentEditPanel {

    MappingDiagram diagram;

    Button removeButton;

    protected void buildUI() {
        ButtonBar buttonBar = new ButtonBar();
        addComponent(buttonBar);
        Button autoMapButton = buttonBar.addButton("Auto Map", FontAwesome.FLASH);
        removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
        removeButton.setEnabled(false);
        autoMapButton.addClickListener(new AutoMapListener());
        removeButton.addClickListener(new RemoveListener());

        HorizontalLayout header1 = new HorizontalLayout();
        header1.setSpacing(true);
        header1.setMargin(new MarginInfo(false, true, false, true));
        header1.setWidth(100f, Unit.PERCENTAGE);
        header1.addComponent(
                new Label("<b>Input Model:</b> &nbsp;" + (component.getInputModel() != null ? component.getInputModel().getName() : "?"),
                        ContentMode.HTML));
        header1.addComponent(
                new Label("<b>Output Model:</b> &nbsp;" + (component.getOutputModel() != null ? component.getOutputModel().getName() : "?"),
                        ContentMode.HTML));
        addComponent(header1);

        HorizontalLayout header2 = new HorizontalLayout();
        header2.setSpacing(true);
        header2.setMargin(new MarginInfo(true, true, true, true));
        header2.setWidth(100f, Unit.PERCENTAGE);
        TextField srcFilter = new TextField();
        srcFilter.setInputPrompt("Filter");
        srcFilter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        srcFilter.setIcon(FontAwesome.SEARCH);
        srcFilter.setImmediate(true);
        srcFilter.setTextChangeEventMode(TextChangeEventMode.LAZY);
        srcFilter.setTextChangeTimeout(200);
        srcFilter.addTextChangeListener(new FilterInputModelListener());
        header2.addComponent(srcFilter);
        TextField dstFilter = new TextField();
        dstFilter.setInputPrompt("Filter");
        dstFilter.addStyleName(ValoTheme.TEXTFIELD_INLINE_ICON);
        dstFilter.setIcon(FontAwesome.SEARCH);
        dstFilter.setImmediate(true);
        dstFilter.setTextChangeEventMode(TextChangeEventMode.LAZY);
        dstFilter.setTextChangeTimeout(200);
        dstFilter.addTextChangeListener(new FilterOutputModelListener());
        header2.addComponent(dstFilter);
        addComponent(header2);

        Panel panel = new Panel();
        VerticalLayout vlay = new VerticalLayout();
        vlay.setSizeFull();
        diagram = new MappingDiagram(context, component);
        diagram.setSizeFull();
        vlay.addComponent(diagram);
        panel.setContent(vlay);
        panel.setSizeFull();
        addComponent(panel);
        setExpandRatio(panel, 1.0f);
        diagram.addListener(new EventListener());
    }

    protected void autoMap(boolean fuzzy) {
        for (ModelEntity entity1 : component.getInputModel().getModelEntities()) {
            for (ModelAttribute attr : entity1.getModelAttributes()) {
                /* look for exact match first */
                for (ModelEntity entity2 : component.getOutputModel().getModelEntities()) {
                    boolean foundExactMatch = false;
                    for (ModelAttribute attr2 : entity2.getModelAttributes()) {
                        foundExactMatch |= autoMap(entity1, entity2, attr, attr2, fuzzy, true);
                    }

                    if (!foundExactMatch) {
                        for (ModelAttribute attr2 : entity2.getModelAttributes()) {
                            autoMap(entity1, entity2, attr, attr2, fuzzy, false);
                        }
                    }

                }

            }
        }
    }

    protected boolean autoMap(ModelEntity entity1, ModelEntity entity2, ModelAttribute attr, ModelAttribute attr2, boolean fuzzy,
            boolean exact) {
        boolean isMapped = false;
        boolean exactMatch = exact && attr.getName().equalsIgnoreCase(attr2.getName()) && entity1.getName().equals(entity2.getName());
        for (ComponentAttributeSetting setting : component.getAttributeSettings()) {
            if (setting.getName().equals(Mapping.ATTRIBUTE_MAPS_TO) && setting.getValue().equals(attr2.getId())) {
                isMapped = true;
                break;
            }
        }
        if (!isMapped && ((fuzzy && fuzzyMatches(attr.getName(), attr2.getName()))
                || ((!exact && attr.getName().equalsIgnoreCase(attr2.getName())) || exactMatch))) {
            ComponentAttributeSetting setting = new ComponentAttributeSetting();
            setting.setAttributeId(attr.getId());
            setting.setComponentId(component.getId());
            setting.setName(Mapping.ATTRIBUTE_MAPS_TO);
            setting.setValue(attr2.getId());
            component.addAttributeSetting(setting);
            context.getConfigurationService().save(setting);
            diagram.markAsDirty();
        }

        return exact;
    }

    protected boolean fuzzyMatches(String str1, String str2) {
        int x = computeLevenshteinDistance(str1, str2);
        return x < 3;
    }

    protected int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
        int[][] distance = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            distance[i][0] = i;
        }
        for (int j = 1; j <= str2.length(); j++) {
            distance[0][j] = j;
        }

        for (int i = 1; i <= str1.length(); i++) {
            for (int j = 1; j <= str2.length(); j++) {
                distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
                        distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));
            }
        }

        return distance[str1.length()][str2.length()];
    }

    protected int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }

    class EventListener implements Listener {
        public void componentEvent(Event event) {
            if (event instanceof SelectEvent) {
                SelectEvent selectEvent = (SelectEvent) event;
                removeButton.setEnabled(selectEvent.getSelectedSourceId() != null);
            }
        }
    }

    class RemoveListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            diagram.removeSelected();
            removeButton.setEnabled(false);
        }
    }

    class AutoMapListener implements ClickListener {
        public void buttonClick(ClickEvent event) {
            autoMap(false);
            autoMap(true);
        }
    }

    class FilterInputModelListener implements TextChangeListener {
        public void textChange(TextChangeEvent event) {
            diagram.filterInputModel((String) event.getText());
        }
    }

    class FilterOutputModelListener implements TextChangeListener {
        public void textChange(TextChangeEvent event) {
            diagram.filterOutputModel((String) event.getText());
        }
    }
}
