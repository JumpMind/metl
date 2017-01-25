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
import java.util.Date;
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

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FileDownloader;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.StreamResource;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.Tree.CollapseEvent;
import com.vaadin.ui.Tree.ExpandEvent;
import com.vaadin.ui.TreeTable;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

@UiComponent
@Scope("ui")
@TopBarLink(id = "exploreDirectories", category = Category.Explore, menuOrder = 20, name = "Directory", icon = FontAwesome.DATABASE)
public class ExploreDirectoryView extends VerticalLayout implements View {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ExploreDirectoryView.class);

    @Autowired
    ApplicationContext context;

    TreeTable table;

    public ExploreDirectoryView() {
        setSizeFull();
        setMargin(false);

        ButtonBar buttonBar = new ButtonBar();
        Button refreshButton = buttonBar.addButton("Refresh", FontAwesome.REFRESH);
        refreshButton.addClickListener(event -> refresh());
        addComponent(buttonBar);

        table = new TreeTable();
        table.setSizeFull();
        table.addExpandListener(event -> expanded(event));
        table.addCollapseListener(event -> collapsed(event));
        table.setSelectable(true);
        table.addContainerProperty("name", String.class, null);
        table.addGeneratedColumn("name", (source, itemId, propertyId) -> fileLinkComponent(source, itemId, propertyId));
        table.setColumnHeader("name", "");
        table.addContainerProperty("lastModified", Date.class, null);
        table.setColumnHeader("lastModified", "Date Modified");
        table.setColumnWidth("lastModified", 150);
        table.addContainerProperty("size", Long.class, null);
        table.setColumnHeader("size", "Size (bytes)");
        table.setColumnExpandRatio("name", 1);
        table.setCellStyleGenerator((source, itemId, propertyId) -> cellStyle(source, itemId, propertyId));
        addComponent(table);
        setExpandRatio(table, 1);
        
    }
    
    @PostConstruct
    protected void init() {
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

    protected Component fileLinkComponent(Table source, Object itemId, Object propertyId) {
        if (itemId instanceof FileInfo) {
            final FileInfo file = (FileInfo) itemId;
            if (!file.isDirectory()) {
                final Button button = new Button(file.getName());
                button.addStyleName(ValoTheme.BUTTON_LINK);
                button.addStyleName(ValoTheme.BUTTON_SMALL);
                button.setIcon(FontAwesome.FILE);
                StreamResource resource = new StreamResource(() -> stream(file), file.getName());
                FileDownloader fileDownloader = new FileDownloader(resource);
                fileDownloader.extend(button);
                return button;
            } else {
                return new Label(file.getName());
            }
        } else {
            return new Label(((DirectoryResource) itemId).getName());

        }
    }

    protected InputStream stream(FileInfo file) {
        DirectoryResource resource = getDirectoryResource(file);
        return resource.getDirectory().getInputStream(file.getRelativePath(), true);
    }

    protected String cellStyle(Table source, Object itemId, Object propertyId) {
        String styleName = null;
        if (itemId instanceof FileInfo) {
            styleName = ((FileInfo) itemId).isDirectory() ? "folder" : null;
        } else if (itemId instanceof DirectoryResource) {
            styleName = "folder";
        }
        return styleName;
    }

    protected void collapsed(CollapseEvent event) {
        Object item = event.getItemId();
        table.setItemIcon(item, Icons.FOLDER_CLOSED);
        Collection<?> children = table.getChildren(item);
        if (children != null) {
            for (Object object : new HashSet<>(children)) {
                table.removeItem(object);
            }
        }
    }

    protected void expanded(ExpandEvent event) {
        Object item = event.getItemId();
        table.setItemIcon(item, Icons.FOLDER_OPEN);
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
            return getDirectoryResource(table.getParent(item));
        }
    }

    protected void addChildren(Object item, List<FileInfo> files) {
        for (FileInfo fileInfo : files) {
            table.addItem(new Object[] { new Date(fileInfo.getLastUpdated()), fileInfo.getSize() }, fileInfo);
            table.setParent(fileInfo, item);
            if (fileInfo.isDirectory()) {
                table.setItemIcon(fileInfo, Icons.FOLDER_CLOSED);
                table.setChildrenAllowed(fileInfo, true);
            } else {
                //table.setItemIcon(fileInfo, FontAwesome.FILE);
                table.setChildrenAllowed(fileInfo, false);
            }
        }

    }

    protected void refresh() {
        table.removeAllItems();

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
            table.addItem(new Object[] { null, null }, resource);
            table.setItemIcon(resource, Icons.FOLDER_CLOSED);
            table.setChildrenAllowed(resource, true);
        }

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
