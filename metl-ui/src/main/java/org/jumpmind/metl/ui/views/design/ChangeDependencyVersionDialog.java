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

import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.ResizableDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;

public class ChangeDependencyVersionDialog extends ResizableDialog  {

    final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1L;
    IConfigurationService configService;
    RadioButtonGroup<ProjectVersion> optionGroup;
    ProjectVersionDepends dependency;
    DesignNavigator designNavigator;

    public ChangeDependencyVersionDialog(ApplicationContext context, Object selectedElement, DesignNavigator designNavigator) {
        super("Change Dependency Version");
        this.configService = context.getConfigurationService();
        this.designNavigator = designNavigator;
        initDialog(selectedElement);
    }

    public static void show(DesignNavigator designNavigator, ApplicationContext context, Object selectedElement) {
        new ChangeDependencyVersionDialog(context, selectedElement, designNavigator).open();
    }

    private void initDialog(Object selectedItem) {
        dependency = (ProjectVersionDepends) selectedItem;
        
        setWidth("400px");
        setHeight("600px");
        VerticalLayout vLayout = new VerticalLayout();
        vLayout.setSizeFull();
        vLayout.setMargin(true);
        ProjectVersion sourceProjectVersion = configService.findProjectVersion(dependency.getProjectVersionId());
        ProjectVersion targetProjectVersion = configService.findProjectVersion(dependency.getTargetProjectVersionId());
        FormLayout form = buildForm(sourceProjectVersion, targetProjectVersion);
        vLayout.add(form);

        vLayout.addAndExpand(buildPossibleTargetVersions(targetProjectVersion));
        addComponentAtIndex(1, vLayout);
        add(buildButtonBar());  
    }
    
    protected VerticalLayout buildPossibleTargetVersions(ProjectVersion targetProjectVersion) {

        Scroller possibleTargetVersionsPanel = new Scroller();        
        possibleTargetVersionsPanel.setSizeFull();
        
        optionGroup = new RadioButtonGroup<ProjectVersion>();
        optionGroup.setLabel("Project Version");
        optionGroup.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
        optionGroup.addClassName("indent");
        optionGroup.setRenderer(new ComponentRenderer<Span, ProjectVersion>(version -> new Span(version.getVersionLabel())));
        optionGroup.setItemEnabledProvider(version -> !targetProjectVersion.getId().equalsIgnoreCase(version.getId()));
        optionGroup.setItems(configService.findProjectVersionsByProject(targetProjectVersion.getProject()));

        possibleTargetVersionsPanel.setContent(optionGroup);
        
        VerticalLayout possibleTargetVersionsLayout = new VerticalLayout(new H3("Available Target Versions"),
                possibleTargetVersionsPanel);
        possibleTargetVersionsLayout.setSizeFull();
        return possibleTargetVersionsLayout;
    }
    
    protected FormLayout buildForm(ProjectVersion sourceProjectVersion, ProjectVersion targetProjectVersion) {
        FormLayout form = new FormLayout();
        TextField sourceProjectNameField = new TextField("Source Project");
        sourceProjectNameField.setValue(sourceProjectVersion.getProject().getName());
        sourceProjectNameField.setEnabled(false);
        form.add(sourceProjectNameField);
        TextField targetProjectNameField = new TextField("Target Project");
        targetProjectNameField.setValue(targetProjectVersion.getProject().getName());
        targetProjectNameField.setEnabled(false);
        form.add(targetProjectNameField);
        TextField currentDependencyVersion = new TextField("Current Dependency Version");
        currentDependencyVersion.setValue(targetProjectVersion.getVersionLabel());
        currentDependencyVersion.setEnabled(false);
        form.add(currentDependencyVersion);

        return form;
    }

    protected HorizontalLayout buildButtonBar() {
        Button cancelButton = new Button("Cancel", e->cancel());
        Button changeButton = new Button("Change", e->change());
        changeButton.setDisableOnClick(true);
        changeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        changeButton.addClickShortcut(Key.ENTER);
        return buildButtonFooter(cancelButton, changeButton);        
    }

    protected void change() {
    	ProjectVersion selectedVersion = optionGroup.getValue();
        String selectedVersionId = selectedVersion != null ? selectedVersion.getId() : null;
        configService.updateProjectVersionDependency(dependency, selectedVersionId);  
        designNavigator.refresh();
        close();
    }
    
    protected void cancel() {
        close();
    }
    
}
