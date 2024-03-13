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

import org.jumpmind.metl.core.model.WhereUsed;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.metl.ui.common.ButtonBar;
import org.jumpmind.metl.ui.common.ExportDialog;
import org.jumpmind.metl.ui.common.Icons;
import org.jumpmind.vaadin.ui.common.IUiPanel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.SelectionMode;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class WhereUsedPanel extends VerticalLayout implements IUiPanel {

    private static final long serialVersionUID = 1L;

    final Logger log = LoggerFactory.getLogger(getClass());

    ApplicationContext context;

    DesignNavigator designNavigator;
    
    String whereUsedType;
    
    String whereUsedObjectId;
    
    String whereUsedObjectName;

    Grid<WhereUsed> componentWhereUsedGrid;
    
    Button openButton;
    
    public WhereUsedPanel(String whereUsedType, String whereUsedObjectId, String whereUsedObjectName, ApplicationContext context, DesignNavigator projectNavigator) {
        this.setSizeFull();
        this.setPadding(false);
        this.setSpacing(false);
        this.context = context;
        this.designNavigator = projectNavigator;
        this.whereUsedType = whereUsedType;
        this.whereUsedObjectId = whereUsedObjectId;
        this.whereUsedObjectName = whereUsedObjectName;
        
        H3 header = new H3(whereUsedType + " - Where Used");
        header.getStyle().set("padding", "16px");
        add(header);

        ButtonBar buttonBar = new ButtonBar();
        add(buttonBar);
        openButton = buttonBar.addButton("Open Flow", Icons.FLOW, (event)->openFlow()); 
        buttonBar.addButtonRight("Export", VaadinIcon.DOWNLOAD, (event)->export());
        
        componentWhereUsedGrid = new Grid<WhereUsed>();
        componentWhereUsedGrid.setSelectionMode(SelectionMode.SINGLE);
        componentWhereUsedGrid.setAllRowsVisible(true);
        componentWhereUsedGrid.setWidthFull();
        componentWhereUsedGrid.addColumn(WhereUsed::getObjectId).setHeader("Id").setVisible(false); // adding hidden column of id because the export does not allow only one column in output
        componentWhereUsedGrid.addColumn(WhereUsed::getProjectName).setHeader("Project");
        
        if (!"ProjectVersion".equals(whereUsedType)) {
        	componentWhereUsedGrid.addColumn(WhereUsed::getFlowName).setHeader("Flow");
        	componentWhereUsedGrid.addColumn(WhereUsed::getComponentName).setHeader("Component");
        }
        
        add(componentWhereUsedGrid);

        VerticalLayout spacer = new VerticalLayout();
        addAndExpand(spacer);

        populateContainer();
        setButtonsEnabled();
    }
    
    protected void openFlow() {
    	String flowId = "";
    	String flowName = "";
    	WhereUsed selected = componentWhereUsedGrid.getSelectionModel().getFirstSelectedItem().orElse(null);
    	if (selected != null) {
    		flowId = selected.getObjectId();
    		flowName = selected.getFlowName();
            EditFlowPanel flowLayout = new EditFlowPanel(context, flowId, designNavigator, designNavigator.tabs);
            designNavigator.tabs.addCloseableTab(flowId, flowName, new Icon(Icons.FLOW), flowLayout);
            designNavigator.tabs.setSelectedTab(flowLayout);
    	}
    }

    protected void export() {
        ExportDialog.show(context, componentWhereUsedGrid);
    }
    
    protected void populateContainer() {
        IConfigurationService configurationService = context.getConfigurationService();
        List<WhereUsed> whereUsed = null;
        if ("Model".equals(whereUsedType)) {
        	whereUsed = configurationService.findModelWhereUsed(whereUsedObjectId);
        } else if ("Resource".equals(whereUsedType)) {
        	whereUsed = configurationService.findResourceWhereUsed(whereUsedObjectId);
        } else if ("Flow".equals(whereUsedType)) {
        	whereUsed = configurationService.findFlowWhereUsed(whereUsedObjectId);
        } else if ("ProjectVersion".equals(whereUsedType)) {
        	whereUsed = configurationService.findProjectVersionWhereUsed(whereUsedObjectId);
        }
        componentWhereUsedGrid.setItems(whereUsed);
    }

    @Override
    public void selected() {
    }

    @Override
    public void deselected() {
    }

    @Override
    public boolean closing() {
        return true;
    }
    
    protected void setButtonsEnabled() {
        boolean enableButton = true;
        if ("ProjectVersion".equals(whereUsedType)) {
        	enableButton = false;
        }
        openButton.setEnabled(enableButton);
    }
}
