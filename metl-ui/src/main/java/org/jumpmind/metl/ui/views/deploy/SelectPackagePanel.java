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
package org.jumpmind.metl.ui.views.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class SelectPackagePanel extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;
    
    IConfigurationService configService;
    
    Grid grid = new Grid();
    
    Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();
    
    List<ReleasePackage> selectedPackages = new ArrayList<ReleasePackage>();
    
    public SelectPackagePanel(ApplicationContext context, String introText) {
        this.context = context;
        this.configService = context.getConfigurationService();
        buildPanel(introText);
    }
    
    public List<ReleasePackage> getSelectedPackages() {
        return selectedPackages;
    }

    protected void buildPanel(String introText) {
        this.setSpacing(true);
        this.setSizeFull();
        this.addComponent(new Label(introText));
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.MULTI);
        BeanItemContainer<ReleasePackage> container = new BeanItemContainer<>(ReleasePackage.class);
        container.addAll(configService.findReleasePackages());
        grid.setContainerDataSource(container);
        grid.setColumns("name", "versionLabel", "releaseDate");
        grid.addSelectionListener((e) -> rowSelected());        
        this.addComponent(grid);
        this.setExpandRatio(grid, 1);        
    }
    
    protected void rowSelected() {
        selectedPackages.clear();
        for (Object object : grid.getSelectedRows()) {
            selectedPackages.add((ReleasePackage) object);
        }
    }
}
