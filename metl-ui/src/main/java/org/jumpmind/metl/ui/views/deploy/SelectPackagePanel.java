package org.jumpmind.metl.ui.views.deploy;

import java.util.HashMap;
import java.util.Map;

import org.jumpmind.db.platform.IDatabasePlatform;
import org.jumpmind.metl.core.model.ReleasePackage;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;

import com.vaadin.data.util.BeanItemContainer;
import com.vaadin.ui.Grid;
import com.vaadin.ui.Grid.SelectionMode;
import com.vaadin.ui.Label;
import com.vaadin.ui.Tree;
import com.vaadin.ui.VerticalLayout;

public class SelectPackagePanel extends VerticalLayout {

    private static final long serialVersionUID = 1L;

    ApplicationContext context;
    
    IConfigurationService configService;
    
    Tree tree = new Tree();
    
    Map<Object, IDatabasePlatform> platformByItemId = new HashMap<Object, IDatabasePlatform>();
    
    public SelectPackagePanel(ApplicationContext context, String introText) {
        
        this.context = context;
        this.configService = context.getConfigurationService();
        buildPanel(introText);
    }
    
    protected void buildPanel(String introText) {
        
        this.setSpacing(true);
        this.setSizeFull();
        this.addComponent(new Label(introText));
        Grid grid = new Grid();
        grid.setSizeFull();
        grid.setSelectionMode(SelectionMode.MULTI);
        BeanItemContainer<ReleasePackage> container = new BeanItemContainer<>(ReleasePackage.class);
        container.addAll(configService.findReleasePackages());
        grid.setContainerDataSource(container);
        grid.setColumns("name", "versionLabel", "releaseDate");
        this.addComponent(grid);
        this.setExpandRatio(grid, 1);        
    }
}
