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
package org.jumpmind.metl.ui.common;

import java.util.List;

import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.Tree;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class SelectProjectVersionDialog extends ResizableWindow {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;

    Tree tree = new Tree();

    IProjectVersionSelectListener listener;

    @SuppressWarnings({ "serial" })
    public SelectProjectVersionDialog(ApplicationContext context, Project projectToExclude,
            String caption, String introText) {
        super(caption);
        this.context = context;

        tree.setMultiSelect(false);
        tree.addContainerProperty("name", String.class, "");
        tree.setItemCaptionPropertyId("name");
        tree.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        addProjects(projectToExclude);

        setWidth(600.0f, Unit.PIXELS);
        setHeight(600.0f, Unit.PIXELS);

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setSizeFull();
        layout.addComponent(new Label(introText));

        Panel scrollable = new Panel();
        scrollable.addStyleName(ValoTheme.PANEL_BORDERLESS);
        scrollable.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        scrollable.setSizeFull();
        scrollable.setContent(tree);
        layout.addComponent(scrollable);
        layout.setExpandRatio(scrollable, 1.0f);
        addComponent(layout, 1);

        Button cancelButton = new Button("Cancel");
        Button selectButton = new Button("Select");
        addComponent(buildButtonFooter(cancelButton, selectButton));

        cancelButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                close();
            }
        });

        selectButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                if (tree.getValue() instanceof ProjectVersion) {
                    listener.selected((ProjectVersion) tree.getValue());
                    close();
                }
            }
        });
    }

    public static void show(ApplicationContext context, Project projectToExclude,
            IProjectVersionSelectListener listener, String introText) {
        SelectProjectVersionDialog dialog = new SelectProjectVersionDialog(context,
                projectToExclude, "Select Version", introText);
        dialog.setProjectVersionSelectListener(listener);
        UI.getCurrent().addWindow(dialog);
    }

    protected void addProjects(Project projectToExclude) {
        List<Project> projects = context.getConfigurationService().findProjects();
        for (Project project : projects) {
            if (!projectToExclude.equals(project)) {
                addItem(project, project.getName(), Icons.PROJECT, null, true);
                for (ProjectVersion version : project.getProjectVersions()) {
                    addItem(version, version.getVersionLabel(), Icons.VERSION, project, false);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addItem(Object itemId, String name, FontAwesome icon, Object parent,
            boolean areChildrenAllowed) {
        tree.addItem(itemId);
        tree.getContainerProperty(itemId, "name").setValue(name);
        tree.setItemIcon(itemId, icon);
        tree.setParent(itemId, parent);
        tree.setChildrenAllowed(itemId, areChildrenAllowed);
    }

    public void setProjectVersionSelectListener(IProjectVersionSelectListener listener) {
        this.listener = listener;
    }

    public interface IProjectVersionSelectListener {
        public void selected(ProjectVersion version);
    }

}