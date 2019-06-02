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

import java.util.List;

import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.data.Item;
import com.vaadin.data.util.IndexedContainer;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.AbstractSelect.ItemCaptionMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class ChangeDependencyVersionDialog extends ResizableWindow  {

    final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1L;
    IConfigurationService configService;
    OptionGroup optionGroup;
    ProjectVersionDepends dependency;
    DesignNavigator designNavigator;

    public ChangeDependencyVersionDialog(ApplicationContext context, Object selectedElement, DesignNavigator designNavigator) {
        super("Change Dependency Version");
        this.configService = context.getConfigurationService();
        this.designNavigator = designNavigator;
        initWindow(selectedElement);
    }

    public static void show(DesignNavigator designNavigator, ApplicationContext context, Object selectedElement) {
        ChangeDependencyVersionDialog dialog = new ChangeDependencyVersionDialog(context, selectedElement, designNavigator);
        UI.getCurrent().addWindow(dialog);
    }

    private void initWindow(Object selectedItem) {
        dependency = (ProjectVersionDepends) selectedItem;
        
        setWidth(400.0f, Unit.PIXELS);
        setHeight(600.0f, Unit.PIXELS);
        VerticalLayout vLayout = new VerticalLayout();
        vLayout.setSizeFull();
        vLayout.setMargin(true);
        ProjectVersion sourceProjectVersion = configService.findProjectVersion(dependency.getProjectVersionId());
        ProjectVersion targetProjectVersion = configService.findProjectVersion(dependency.getTargetProjectVersionId());
        FormLayout form = buildForm(sourceProjectVersion, targetProjectVersion);
        vLayout.addComponent(form);

        Panel projectVersionPanel = new Panel();
        projectVersionPanel.setSizeFull();
        projectVersionPanel.setContent(buildPossibleTargetVersions(targetProjectVersion));
        vLayout.addComponent(projectVersionPanel);
        addComponent(vLayout,1); 
        addComponent(buildButtonBar());
        vLayout.setExpandRatio(projectVersionPanel, 1);       
    }
    
    @SuppressWarnings("unchecked")
    protected Panel buildPossibleTargetVersions(ProjectVersion targetProjectVersion) {

        Panel possibleTargetVersionsPanel = new Panel("Available Target Versions");        
        possibleTargetVersionsPanel.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        possibleTargetVersionsPanel.setSizeFull();

        IndexedContainer container = new IndexedContainer();
        optionGroup = new OptionGroup("Project Version", container);
        optionGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        optionGroup.setItemCaptionMode(ItemCaptionMode.PROPERTY);
        optionGroup.setItemCaptionPropertyId("versionLabel");
        optionGroup.addStyleName("indent");

        List<ProjectVersion> projectVersions = configService.findProjectVersionsByProject(targetProjectVersion.getProject());        
        container.addContainerProperty("versionLabel", String.class, null); 
        for (ProjectVersion version : projectVersions) {
            Item item = container.addItem(version.getId());
            item.getItemProperty("versionLabel").setValue(version.getVersionLabel()); 
            if (targetProjectVersion.getId().equalsIgnoreCase(version.getId())) {
                optionGroup.setItemEnabled(version.getId(), false);
            }
        }
        possibleTargetVersionsPanel.setContent(optionGroup);
        return possibleTargetVersionsPanel;
    }
    
    protected FormLayout buildForm(ProjectVersion sourceProjectVersion, ProjectVersion targetProjectVersion) {
        FormLayout form = new FormLayout();
        form.addStyleName(ValoTheme.FORMLAYOUT_LIGHT);
        form.setMargin(true);
        TextField sourceProjectNameField = new TextField("Source Project");
        sourceProjectNameField.setValue(sourceProjectVersion.getProject().getName());
        sourceProjectNameField.setEnabled(false);
        form.addComponent(sourceProjectNameField);
        TextField targetProjectNameField = new TextField("Target Project");
        targetProjectNameField.setValue(targetProjectVersion.getProject().getName());
        targetProjectNameField.setEnabled(false);
        form.addComponent(targetProjectNameField);
        TextField currentDependencyVersion = new TextField("Current Dependency Version");
        currentDependencyVersion.setValue(targetProjectVersion.getVersionLabel());
        currentDependencyVersion.setEnabled(false);
        form.addComponent(currentDependencyVersion);

        return form;
    }

    protected HorizontalLayout buildButtonBar() {
        Button cancelButton = new Button("Cancel", e->cancel());
        Button changeButton = new Button("Change", e->change());
        changeButton.setDisableOnClick(true);
        changeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        changeButton.setClickShortcut(KeyCode.ENTER);
        return buildButtonFooter(cancelButton, changeButton);        
    }

    protected void change() {
        String selectedVersionId = (String) optionGroup.getValue();
        configService.updateProjectVersionDependency(dependency, selectedVersionId);  
        designNavigator.refresh();
        close();
    }
    
    protected void cancel() {
        close();
    }
    
}
