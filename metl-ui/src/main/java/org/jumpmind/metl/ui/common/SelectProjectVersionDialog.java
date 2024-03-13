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

import org.jumpmind.metl.core.model.AbstractNamedObject;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.vaadin.ui.common.ResizableDialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

public class SelectProjectVersionDialog extends ResizableDialog {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;

    TreeGrid<AbstractNamedObject> tree = new TreeGrid<AbstractNamedObject>();
    
    TreeData<AbstractNamedObject> treeData = new TreeData<AbstractNamedObject>();
    
    TreeDataProvider<AbstractNamedObject> treeDataProvider;

    IProjectVersionSelectListener listener;

    @SuppressWarnings({ "serial" })
    public SelectProjectVersionDialog(ApplicationContext context, Project projectToExclude,
            String caption, String introText) {
        super(caption);
        this.context = context;

        tree.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_NO_ROW_BORDERS);
        tree.setSelectionMode(SelectionMode.SINGLE);
        tree.addComponentHierarchyColumn(item -> {
            Icon icon = null;
            Span span = new Span(item.getName());
            if (item instanceof Project) {
                icon = new Icon(Icons.PROJECT);
            } else if (item instanceof ProjectVersion) {
                icon = new Icon(Icons.PROJECT_VERSION);
                span.setText(((ProjectVersion) item).getVersionLabel());
            }
            if (icon != null) {
                return new HorizontalLayout(icon, span);
            }
            return span;
        });
        treeDataProvider = new TreeDataProvider<AbstractNamedObject>(treeData);
        tree.setDataProvider(treeDataProvider);
        addProjects(projectToExclude);

        setWidth("600px");
        setHeight("600px");

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setPadding(false);
        layout.setSizeFull();
        layout.add(new Span(introText), tree);
        add(layout);

        Button cancelButton = new Button("Cancel");
        Button selectButton = new Button("Select");
        buildButtonFooter(cancelButton, selectButton);

        cancelButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            public void onComponentEvent(ClickEvent<Button> event) {
                close();
            }
        });

        selectButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            public void onComponentEvent(ClickEvent<Button> event) {
				AbstractNamedObject selection = tree.getSelectionModel().getFirstSelectedItem().orElse(null);
                if (selection instanceof ProjectVersion) {
                    listener.selected((ProjectVersion) selection);
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
        dialog.open();
    }

    protected void addProjects(Project projectToExclude) {
        List<Project> projects = context.getConfigurationService().findProjects();
        for (Project project : projects) {
            if (!projectToExclude.equals(project)) {
                treeData.addItem(null, project);
                for (ProjectVersion version : project.getProjectVersions()) {
                    treeData.addItem(project, version);
                }
            }
        }
    }

    public void setProjectVersionSelectListener(IProjectVersionSelectListener listener) {
        this.listener = listener;
    }

    public interface IProjectVersionSelectListener {
        public void selected(ProjectVersion version);
    }

}