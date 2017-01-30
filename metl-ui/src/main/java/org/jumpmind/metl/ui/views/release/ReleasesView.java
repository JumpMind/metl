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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.core.model.ProjectVersion;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.model.ReleasePackageProjectVersion;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.persist.IImportExportService;
import org.jumpmind.metl.core.util.AppConstants;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.vaadin.ui.common.CommonUiUtils;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ResourceReference;
import com.vaadin.server.StreamResource;
import com.vaadin.server.StreamResource.StreamSource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Notification.Type;
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

    IConfigurationService configService;

    IImportExportService importExportService;

    public ReleasesView() {
        setSizeFull();
        setMargin(false);
        ButtonBar buttonBar = new ButtonBar();
        addButton = buttonBar.addButton("Add", FontAwesome.PLUS, e -> add());
        editButton = buttonBar.addButton("Edit", FontAwesome.EDIT, e -> edit());
        exportButton = buttonBar.addButton("Export", FontAwesome.DOWNLOAD, e -> export());
        archiveButton = buttonBar.addButton("Archive", FontAwesome.ARCHIVE, e -> archive());
        finalizeButton = buttonBar.addButton("Finalize", FontAwesome.CUBE, e -> finalize());
        addComponent(buttonBar);
        enableDisableButtonsForSelectionSize(0);
        grid = new Grid();
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.MULTI);
        grid.addSelectionListener((e) -> rowSelected());
        container = new BeanItemContainer<>(ReleasePackage.class);
        grid.setContainerDataSource(container);
        grid.setColumns("name", "versionLabel", "releaseDate", "released");
        addComponent(grid);
        setExpandRatio(grid, 1);
    }

    @Override
    public void updated(ReleasePackage releasePackage) {
        refresh();
    }

    @PostConstruct
    protected void init() {
        configService = context.getConfigurationService();
        importExportService = context.getImportExportService();
    }

    protected void refresh() {
        container.removeAllItems();
        List<ReleasePackage> releasePackages = configService.findReleasePackages();
        container.addAll(releasePackages);
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
                for (ReleasePackageProjectVersion rppv : releasePackage.getProjectVersions()) {
                    ProjectVersion projectVersion = configService
                            .findProjectVersion(rppv.getProjectVersionId());
                    projectVersion.setArchived(true);
                    configService.save(projectVersion);
                }
            } else {
                CommonUiUtils.notify(String.format(String.format(
                        "Release Package %s is not released, it cannot be archived.  Skipping this release package",
                        releasePackage.getName())), Type.WARNING_MESSAGE);
            }
        }
    }

    protected void export() {
        // TODO this should be release package name and then refreshed to
        // releaespackage
        // TODO we should let people export an entire list of release packages
        // vs one
        ReleasePackage releasePackage = getFirstSelectedReleasePackage();
        String export = importExportService.exportReleasePackage(releasePackage.getId(),
                AppConstants.SYSTEM_USER);
        downloadExport(export, releasePackage.getName());
    }

    protected void finalize() {
        // TODO prompt for confirmation
        Collection<Object> collection = grid.getSelectedRows();
        Iterator<Object> itr = collection.iterator();
        while (itr.hasNext()) {
            ReleasePackage releasePackage = (ReleasePackage) itr.next();
            if (releasePackage.getReleaseDate() != null) {
                CommonUiUtils.notify(String.format(
                        "Release Package %s is already released.  It cannot be re-released.  Skipping this release package.",
                        releasePackage.getName()));
            } else {
                List<ReleasePackageProjectVersion> rppvs = releasePackage.getProjectVersions();
                for (ReleasePackageProjectVersion rppv : rppvs) {
                    ProjectVersion original = configService.findProjectVersion(rppv.getProjectVersionId());
                    original.setName(releasePackage.getVersionLabel());
                    
                    
                }
            }
        }
    }

    protected void rowSelected() {
        Collection<Object> packages = grid.getSelectedRows();
        enableDisableButtonsForSelectionSize(packages.size());
    }

    protected void enableDisableButtonsForSelectionSize(int nbrRowsSelected) {
        if (nbrRowsSelected == 0) {
            editButton.setEnabled(false);
            exportButton.setEnabled(false);
            archiveButton.setEnabled(false);
            finalizeButton.setEnabled(false);
        } else {
            editButton.setEnabled(true);
            exportButton.setEnabled(true);
            archiveButton.setEnabled(true);
            finalizeButton.setEnabled(true);
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
}
