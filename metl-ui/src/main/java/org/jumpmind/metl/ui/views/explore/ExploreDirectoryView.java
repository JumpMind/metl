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
import org.jumpmind.metl.ui.common.View;
import org.jumpmind.vaadin.ui.common.UiComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.CollapseEvent;
import com.vaadin.flow.component.treegrid.ExpandEvent;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

@UiComponent
@Scope("ui")
@TopBarLink(id = "exploreDirectories", category = Category.Explore, menuOrder = 20, name = "Directory", icon = VaadinIcon.DATABASE)
@Route("exploreDirectories")
public class ExploreDirectoryView extends VerticalLayout implements BeforeEnterObserver, View {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ExploreDirectoryView.class);

    @Autowired
    ApplicationContext context;

    TreeGrid<Object> grid;

    public ExploreDirectoryView() {
        setSizeFull();
        setMargin(false);

        ButtonBar buttonBar = new ButtonBar();
        Button refreshButton = buttonBar.addButton("Refresh", VaadinIcon.REFRESH);
        refreshButton.addClickListener(event -> refresh());
        add(buttonBar);

        grid = new TreeGrid<Object>();
        grid.setSizeFull();
        grid.addExpandListener(event -> expanded(event));
        grid.addCollapseListener(event -> collapsed(event));
        grid.addComponentColumn(item -> fileLinkComponent(item)).setHeader("").setFlexGrow(1);
        grid.addColumn(item -> {
            if (item instanceof FileInfo) {
                return ((FileInfo) item).getLastUpdated();
            }
            return null;
        }).setHeader("Date Modified").setWidth("150px");
        grid.addColumn(item -> {
            if (item instanceof FileInfo) {
                return ((FileInfo) item).getSize();
            }
            return null;
        }).setHeader("Size (bytes)");
        grid.setClassNameGenerator(itemId -> cellStyle(itemId));
        addAndExpand(grid);
        
    }
    
    @PostConstruct
    protected void init() {
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        refresh();
    }

    protected Component fileLinkComponent(Object itemId) {
        if (itemId instanceof FileInfo) {
            final FileInfo file = (FileInfo) itemId;
            if (!file.isDirectory()) {
                final Button button = new Button(file.getName());
                button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
                button.setIcon(new Icon(VaadinIcon.FILE));
                StreamResource resource = new StreamResource(file.getName(), () -> stream(file));
                Anchor fileDownloader = new Anchor(resource, null);
                fileDownloader.add(button);
                return fileDownloader;
            } else {
                Icon icon = new Icon(grid.isExpanded(itemId) ? Icons.FOLDER_OPEN : Icons.FOLDER_CLOSED);
                return new HorizontalLayout(icon, new Span(file.getName()));
            }
        } else {
            Icon icon = new Icon(grid.isExpanded(itemId) ? Icons.FOLDER_OPEN : Icons.FOLDER_CLOSED);
            return new HorizontalLayout(icon, new Span(((DirectoryResource) itemId).getName()));

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

    protected void collapsed(CollapseEvent<Object, TreeGrid<Object>> event) {
        Collection<Object> items = event.getItems();
        if (!items.isEmpty()) {
            Object item = items.iterator().next();
            Collection<?> children = grid.getTreeData().getChildren(item);
            if (children != null) {
                for (Object object : new HashSet<>(children)) {
                    grid.getTreeData().removeItem(object);
                }
            }
            grid.getDataProvider().refreshAll();
        }
    }

    protected void expanded(ExpandEvent<Object, TreeGrid<Object>> event) {
        Collection<Object> items = event.getItems();
        if (!items.isEmpty()) {
            Object item = items.iterator().next();
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
