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
package org.jumpmind.metl.ui.views.release;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ProjectVersionDepends;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.Rppv;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.persist.ReleasePackageProjectVersionSorter;
import org.jumpmind.metl.core.util.AppConstants;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.InProgressDialog;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.ConfirmDialog;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.event.ItemClickEvent;
import com.vaadin.event.SelectionEvent;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.shared.data.sort.SortDirection;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Notification.Type;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.VerticalLayout;

@UiComponent
@Scope(value = "ui")
@TopBarLink(id = "release", category = Category.Release, menuOrder = 20, name = "Release", icon = FontAwesome.CUBE)
public class ReleasesView extends VerticalLayout implements View, IReleasePackageListener {

    private static final long serialVersionUID = 1L;

    protected static final Logger log = LoggerFactory.getLogger(ReleasesView.class);

    @Autowired
    ApplicationContext context;

    Button addButton;

    Button editButton;

    Button exportButton;

    Button archiveButton;

    Button finalizeButton;

    Grid grid;

    BeanItemContainer<ReleasePackage> container;

    IConfigurationService configurationService;

    IImportExportService importExportService;
    
    ProgressBar progressBar;

    public ReleasesView() {
        setSizeFull();
        setMargin(false);
        ButtonBar buttonBar = new ButtonBar();
        addButton = buttonBar.addButton("Add", FontAwesome.PLUS, e -> add());
        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT, e -> edit());
        exportButton = buttonBar.addButton("Export", FontAwesome.DOWNLOAD, e -> export());
        archiveButton = buttonBar.addButton("Archive", FontAwesome.ARCHIVE, e -> archive());
        // TODO add support for the archive button
        archiveButton.setVisible(false);
        finalizeButton = buttonBar.addButton("Finalize", FontAwesome.CUBE, e -> finalize());
        addComponent(buttonBar);
        enableDisableButtonsForSelectionSize(0);
        grid = new Grid();
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.MULTI);
        grid.addItemClickListener(e->rowClicked(e));
        grid.addSelectionListener((e) -> rowSelected(e));
        container = new BeanItemContainer<>(ReleasePackage.class);
        grid.setContainerDataSource(container);
        grid.setColumns("name", "versionLabel", "releaseDate", "released");
        grid.sort("releaseDate", SortDirection.DESCENDING);
        addComponent(grid);
        setExpandRatio(grid, 1);
        progressBar = new ProgressBar(0.0f);
    }

    @Override
    public void updated(ReleasePackage releasePackage) {
        refresh();
    }
    
    protected void rowClicked(ItemClickEvent event) {
        ReleasePackage object =  (ReleasePackage)event.getItemId();
        if (grid.getSelectedRows().contains(object)) {
            grid.deselect(object);
        } else if (event.isCtrlKey() || event.isAltKey() || event.isShiftKey()) {
            grid.select(object);
        } else {
            grid.deselectAll();
            grid.select(object);
        }
    }

    @PostConstruct
    protected void init() {
        configurationService = context.getConfigurationService();
        importExportService = context.getImportExportService();
    }

    protected void refresh() {
        Collection<Object> selected = grid.getSelectedRows();
        container.removeAllItems();
        List<ReleasePackage> releasePackages = configurationService.findReleasePackages();
        Collections.sort(releasePackages, Collections.reverseOrder(new Comparator<ReleasePackage>() {
            public int compare(ReleasePackage o1, ReleasePackage o2) {
                Date releaseDate1 = o1.getReleaseDate();
                if (releaseDate1 == null) {
                    releaseDate1 = new Date();
                }
                Date releaseDate2 = o2.getReleaseDate();
                if (releaseDate2 == null) {
                    releaseDate2 = new Date();
                }
                return releaseDate1.compareTo(releaseDate2);
            }
        }));
        container.addAll(releasePackages);
        for (Object s : selected) {
            grid.select(s);    
        }                
        Collection<Object> packages = grid.getSelectedRows();
        enableDisableButtonsForSelectionSize(packages.size());
        disableFinalizeIfPackageAlreadyReleased(packages);
    }

    protected void add() {
        new EditReleasePackageDialog(null, context, this).show();
    }

    protected void edit() {
        new EditReleasePackageDialog(getFirstSelectedReleasePackage(), context, this).show();
    }

    protected ReleasePackage getFirstSelectedReleasePackage() {
        Collection<Object> collection = grid.getSelectedRows();
        if (collection.size() > 0) {
            return (ReleasePackage) collection.iterator().next();
        } else {
            return null;
        }
    }

    protected void archive() {
        Collection<Object> collection = grid.getSelectedRows();
        Iterator<Object> itr = collection.iterator();
        while (itr.hasNext()) {
            ReleasePackage releasePackage = (ReleasePackage) itr.next();
            if (releasePackage.getReleaseDate() != null) {
                for (Rppv rppv : releasePackage.getProjectVersions()) {
                    ProjectVersion projectVersion = configurationService
                            .findProjectVersion(rppv.getProjectVersionId());
                    projectVersion.setArchived(true);
                    configurationService.save(projectVersion);
                }
            } else {
                CommonUiUtils.notify(String.format(String.format(
                        "Release Package %s is not released, it cannot be archived.  Skipping this release package",
                        releasePackage.getName())), Type.WARNING_MESSAGE);
            }
        }
    }

    protected void export() {
        // TODO we should let people export an entire list of release packages
        ReleasePackage releasePackage = getFirstSelectedReleasePackage();
        String export = importExportService.exportReleasePackage(releasePackage.getId(),
                AppConstants.SYSTEM_USER);
        downloadExport(export, releasePackage.getName());
    }

    protected void finalize() {
        ConfirmDialog.show("Release the selected packages?",
                "Are you sure you want to release the selected packages?", () -> {
                    InProgressDialog<Object> dialog = new InProgressDialog<Object>("Finalizing Release Package", 
                            new ReleaseWorker(), context.getBackgroundRefresherService(), "Finalize of Release Package Failed");
                    dialog.show();
                    return true;
                });                
    }
    
    protected void finalizeSelectedReleasePackages() {
                
        Collection<Object> collection = grid.getSelectedRows();
        Iterator<Object> itr = collection.iterator();
        while (itr.hasNext()) {
            ReleasePackage releasePackage = (ReleasePackage) itr.next();
            releasePackage = configurationService.findReleasePackage(releasePackage.getId());
            if (releasePackage.isReleased()) {
                CommonUiUtils.notify(String.format(
                        "The release package '%s:%s' is already released.  It cannot be re-released.  Skipping this release package.",
                        releasePackage.getName(), releasePackage.getVersionLabel()));
            } else {
                releasePackage.setReleaseDate(new Date());
                releasePackage.setReleased(true);
                configurationService.save(releasePackage);
                List<Rppv> rppvs = new ReleasePackageProjectVersionSorter(configurationService).sort(releasePackage);
                Map<String, String> projectVersionDependenciesMap = new HashMap<>();
                for (Rppv rppv : rppvs) {
                    ProjectVersion original = configurationService.findProjectVersion(rppv.getProjectVersionId());
                    if (original.getVersionType()
                            .equalsIgnoreCase(ProjectVersion.VersionType.MASTER.toString())) {
                        ProjectVersion newRelease = configurationService.saveNewVersion(
                                releasePackage.getVersionLabel(), original,
                                ProjectVersion.VersionType.RELEASE.toString());
                        projectVersionDependenciesMap.put(original.getId(), newRelease.getId());
                        configurationService.delete(rppv);
                        rppv.setProjectVersionId(newRelease.getId());
                        configurationService.save(rppv);
                        newRelease.setReleaseDate(new Date());
                        configurationService.save(newRelease);
                    } else {
                        original.setName(releasePackage.getVersionLabel());
                        original.setVersionType(ProjectVersion.VersionType.RELEASE.toString());
                        Date releaseDate = new Date();
                        original.setReleaseDate(releaseDate);
                        configurationService.save(original);
                    }
                }
                
                for (String releasedProjectVersionId : projectVersionDependenciesMap.keySet()) {
                    List<ProjectVersionDepends> needsUpdated = configurationService.findProjectDependenciesThatTarget(releasedProjectVersionId);
                    for (ProjectVersionDepends projectVersionDependency : needsUpdated) {
                        boolean isInRelease = false;
                        for (Rppv rppv2 : rppvs) {
                            if (rppv2.getProjectVersionId().equals(projectVersionDependency.getProjectVersionId())) {
                                isInRelease = true;
                            }
                        }
                        if (isInRelease) {
                            configurationService.updateProjectVersionDependency(projectVersionDependency, projectVersionDependenciesMap.get(releasedProjectVersionId));
                        }
                    }                                            
                }
                context.getDefinitionFactory().refresh();
                refresh();
            } //if release package not released
        } //every release package
    }

    protected void rowSelected(SelectionEvent event) {
        Collection<Object> packages = event.getSelected();
        enableDisableButtonsForSelectionSize(packages.size());
        disableFinalizeIfPackageAlreadyReleased(packages);
    }

    protected void disableFinalizeIfPackageAlreadyReleased(Collection<Object> packages) {
        for (Object pkg : packages) {
            ReleasePackage releasePackage = container.getItem(pkg).getBean();            
            if (releasePackage.isReleased()) {
                finalizeButton.setEnabled(false);
            }
        }
    }
    
    protected void enableDisableButtonsForSelectionSize(int nbrRowsSelected) {

        if (nbrRowsSelected > 0) {
            editButton.setEnabled(true);
            exportButton.setEnabled(true);
            archiveButton.setEnabled(true);
            finalizeButton.setEnabled(true);
            if (nbrRowsSelected > 1) {
                editButton.setEnabled(false);
                exportButton.setEnabled(false);
            }
        } else {
            editButton.setEnabled(false);
            exportButton.setEnabled(false);
            archiveButton.setEnabled(false);
            finalizeButton.setEnabled(false);            
        }
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

    protected void downloadExport(final String export, String filename) {

        StreamSource ss = new StreamSource() {
            private static final long serialVersionUID = 1L;

            public InputStream getStream() {
                try {
                    return new ByteArrayInputStream(export.getBytes(Charset.forName("utf-8")));
                } catch (Exception e) {
                    log.error("Failed to export configuration", e);
                    CommonUiUtils.notify("Failed to export configuration.", Type.ERROR_MESSAGE);
                    return null;
                }
            }
        };
        String datetime = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        StreamResource resource = new StreamResource(ss,
                String.format("%s-config-%s.json", filename, datetime));
        final String KEY = "export";
        setResource(KEY, resource);
        Page.getCurrent().open(ResourceReference.create(resource, this, KEY).getURL(), null);
    }
    
    class ReleaseWorker implements InProgressDialog.InProgressWorker<Object> {

        @Override
        public Object doWork() {
            finalizeSelectedReleasePackages();
            return true;
        }
        
        @Override
        public void doUI(Object data) {
        }
    }
}
