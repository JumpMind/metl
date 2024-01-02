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

import com.vaadin.icons.VaadinIcons;
import com.vaadin.shared.ui.MarginInfo;
import com.vaadin.shared.ui.grid.HeightMode;
import com.vaadin.ui.AbstractLayout;
import com.vaadin.ui.Button;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.components.grid.SingleSelectionModel;
import com.vaadin.ui.themes.ValoTheme;

public class WhereUsedPanel extends Panel implements IUiPanel {

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
        this.context = context;
        this.designNavigator = projectNavigator;
        this.whereUsedType = whereUsedType;
        this.whereUsedObjectId = whereUsedObjectId;
        this.whereUsedObjectName = whereUsedObjectName;
        
        VerticalLayout content = new VerticalLayout();
        setContent(content);
        
        addHeader(whereUsedType + " - Where Used");

        ButtonBar buttonBar = new ButtonBar();
        content.addComponent(buttonBar);
        openButton = buttonBar.addButton("Open Flow", Icons.FLOW, (event)->openFlow()); 
        buttonBar.addButtonRight("Export", VaadinIcons.DOWNLOAD, (event)->export());
        
        componentWhereUsedGrid = new Grid<WhereUsed>();
        componentWhereUsedGrid.setSelectionMode(SelectionMode.SINGLE);
        componentWhereUsedGrid.setHeightMode(HeightMode.ROW);
        componentWhereUsedGrid.setWidth(100, Unit.PERCENTAGE);
        componentWhereUsedGrid.addColumn(WhereUsed::getObjectId).setCaption("Id").setHidden(true); // adding hidden column of id because the export does not allow only one column in output
        componentWhereUsedGrid.addColumn(WhereUsed::getProjectName).setCaption("Project");
        
        if (!"ProjectVersion".equals(whereUsedType)) {
        	componentWhereUsedGrid.addColumn(WhereUsed::getFlowName).setCaption("Flow");
        	componentWhereUsedGrid.addColumn(WhereUsed::getComponentName).setCaption("Component");
        }
        
        content.addComponent(componentWhereUsedGrid);

        VerticalLayout spacer = new VerticalLayout();
        content.addComponent(spacer);
        content.setExpandRatio(spacer, 1);

        populateContainer();
        setButtonsEnabled();
    }
    
    protected void openFlow() {
    	String flowId = "";
    	String flowName = "";
    	WhereUsed selected = ((SingleSelectionModel<WhereUsed>) componentWhereUsedGrid.getSelectionModel()).getSelectedItem().orElse(null);
    	if (selected != null) {
    		flowId = selected.getObjectId();
    		flowName = selected.getFlowName();
            EditFlowPanel flowLayout = new EditFlowPanel(context, flowId, designNavigator, designNavigator.tabs);
            designNavigator.tabs.addCloseableTab(flowId, flowName, Icons.FLOW, flowLayout);
    	}
    }

    protected void export() {
        ExportDialog.show(context, componentWhereUsedGrid);
    }
    
    protected void addHeader(String caption) {
        HorizontalLayout componentHeaderWrapper = new HorizontalLayout();
        componentHeaderWrapper.setMargin(new MarginInfo(false, false, false, true));
        Label componentHeader = new Label(caption);
        componentHeader.addStyleName(ValoTheme.LABEL_H3);
        componentHeader.addStyleName(ValoTheme.LABEL_COLORED);
        componentHeaderWrapper.addComponent(componentHeader);
        ((AbstractLayout)getContent()).addComponent(componentHeaderWrapper);
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
        componentWhereUsedGrid.setHeightByRows(whereUsed.size() > 0 ? whereUsed.size() : 1);
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
