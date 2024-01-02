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
package org.jumpmind.metl.ui.views.explore;

import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import javax.annotation.PostConstruct;

import org.jumpmind.metl.core.model.Agent;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition.ResourceCategory;
import org.jumpmind.metl.core.plugin.XMLResourceDefinition;
import org.jumpmind.metl.core.runtime.AgentRuntime;
import org.jumpmind.metl.core.runtime.IAgentManager;
import org.jumpmind.metl.core.runtime.resource.FileInfo;
import org.jumpmind.metl.core.runtime.resource.IDirectory;
import org.jumpmind.metl.core.runtime.resource.IResourceRuntime;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.Category;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.metl.ui.common.TopBarLink;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.event.CollapseEvent;
import com.vaadin.event.ExpandEvent;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.TreeGrid;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope("ui")
@TopBarLink(id = "exploreDirectories", category = Category.Explore, menuOrder = 20, name = "Directory", icon = VaadinIcons.DATABASE)
public class ExploreDirectoryView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ExploreDirectoryView.class);

    @Autowired
    ApplicationContext context;

    TreeGrid<Object> grid;

    public ExploreDirectoryView() {
        setSizeFull();
        setMargin(false);

        ButtonBar buttonBar = new ButtonBar();
        Button refreshButton = buttonBar.addButton("Refresh", VaadinIcons.REFRESH);
        refreshButton.addClickListener(event -> refresh());
        addComponent(buttonBar);

        grid = new TreeGrid<Object>();
        grid.setSizeFull();
        grid.addExpandListener(event -> expanded(event));
        grid.addCollapseListener(event -> collapsed(event));
        grid.addComponentColumn(item -> fileLinkComponent(item)).setCaption("").setExpandRatio(1);
        grid.addColumn(item -> {
            if (item instanceof FileInfo) {
                return ((FileInfo) item).getLastUpdated();
            }
            return null;
        }).setCaption("Date Modified").setWidth(150);
        grid.addColumn(item -> {
            if (item instanceof FileInfo) {
                return ((FileInfo) item).getSize();
            }
            return null;
        }).setCaption("Size (bytes)");
        grid.setStyleGenerator(itemId -> cellStyle(itemId));
        addComponent(grid);
        setExpandRatio(grid, 1);
        
    }
    
    @PostConstruct
    protected void init() {
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

    protected Component fileLinkComponent(Object itemId) {
        if (itemId instanceof FileInfo) {
            final FileInfo file = (FileInfo) itemId;
            if (!file.isDirectory()) {
                final Button button = new Button(file.getName());
                button.addStyleName(ValoTheme.BUTTON_LINK);
                button.addStyleName(ValoTheme.BUTTON_SMALL);
                button.setIcon(VaadinIcons.FILE);
                StreamResource resource = new StreamResource(() -> stream(file), file.getName());
                FileDownloader fileDownloader = new FileDownloader(resource);
                fileDownloader.extend(button);
                return button;
            } else {
                Label label = new Label(file.getName());
                label.setIcon(grid.isExpanded(itemId) ? Icons.FOLDER_OPEN : Icons.FOLDER_CLOSED);
                return label;
            }
        } else {
            Label label = new Label(((DirectoryResource) itemId).getName());
            label.setIcon(grid.isExpanded(itemId) ? Icons.FOLDER_OPEN : Icons.FOLDER_CLOSED);
            return label;

        }
    }

    protected InputStream stream(FileInfo file) {
        DirectoryResource resource = getDirectoryResource(file);
        return resource.getDirectory().getInputStream(file.getRelativePath(), true);
    }

    protected String cellStyle(Object itemId) {
        String styleName = null;
        if (itemId instanceof FileInfo) {
            styleName = ((FileInfo) itemId).isDirectory() ? "folder" : null;
        } else if (itemId instanceof DirectoryResource) {
            styleName = "folder";
        }
        return styleName;
    }

    protected void collapsed(CollapseEvent<Object> event) {
        Object item = event.getCollapsedItem();
        Collection<?> children = grid.getTreeData().getChildren(item);
        if (children != null) {
            for (Object object : new HashSet<>(children)) {
                grid.getTreeData().removeItem(object);
            }
        }
        grid.getDataProvider().refreshAll();
    }

    protected void expanded(ExpandEvent<Object> event) {
        Object item = event.getExpandedItem();
        DirectoryResource resource = getDirectoryResource(item);
        IDirectory directory = resource.getDirectory();
        try {
            if (item instanceof DirectoryResource) {
                List<FileInfo> files = directory.listFiles("");
                addChildren(item, files);
            } else if (item instanceof FileInfo) {
                List<FileInfo> files = directory.listFiles(((FileInfo) item).getRelativePath());
                addChildren(item, files);
            }
        } catch (UnsupportedOperationException e) {
            log.info("The '{}' resource does not currently support listing files", resource.getName());
        }
    }

    protected DirectoryResource getDirectoryResource(Object item) {
        if (item instanceof DirectoryResource) {
            return (DirectoryResource) item;
        } else {
            return getDirectoryResource(grid.getTreeData().getParent(item));
        }
    }

    protected void addChildren(Object item, List<FileInfo> files) {
        for (FileInfo fileInfo : files) {
            grid.getTreeData().addItem(item, fileInfo);
        }

        grid.getDataProvider().refreshAll();
    }

    protected void refresh() {
        grid.getTreeData().clear();

        List<DirectoryResource> directoryRuntimes = new ArrayList<>();
        IAgentManager agentManager = context.getAgentManager();
        Collection<Agent> agents = agentManager.getAvailableAgents();
        for (Agent agent : agents) {
            AgentRuntime runtime = agentManager.getAgentRuntime(agent.getId());
            Collection<IResourceRuntime> resources = runtime.getDeployedResources();
            for (IResourceRuntime resource : resources) {
                XMLResourceDefinition definition = context.getDefinitionFactory().getResourceDefintion(resource.getResource().getProjectVersionId(), resource.getResource().getType());
                if (definition != null && definition.getResourceCategory() == ResourceCategory.STREAMABLE) {
                    directoryRuntimes.add(new DirectoryResource(agent, resource));
                }
            }
        }

        Collections.sort(directoryRuntimes, new Comparator<DirectoryResource>() {
            @Override
            public int compare(DirectoryResource o1, DirectoryResource o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });

        for (DirectoryResource resource : directoryRuntimes) {
            grid.getTreeData().addItem(null, resource);
        }

        grid.getDataProvider().refreshAll();
    }

    static class DirectoryResource implements Serializable {

        private static final long serialVersionUID = 1L;

        IResourceRuntime resource;
        
        Agent agent;

        public DirectoryResource(Agent agent, IResourceRuntime resource) {
            this.resource = resource;
            this.agent = agent;
        }

        public String getName() {
            return agent.getName() + " > " + this.resource.getResource().getName();
        }

        public IDirectory getDirectory() {
            return this.resource.reference();
        }
    }

}
