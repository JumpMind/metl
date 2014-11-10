package org.jumpmind.symmetric.is.ui.views;

import org.jumpmind.symmetric.is.core.config.FolderType;
import org.jumpmind.symmetric.is.ui.support.AbstractFolderNavigatorLayout;
import org.jumpmind.symmetric.ui.common.IItemUpdatedListener;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;

abstract public class AbstractFolderView extends AbstractFolderNavigatorLayout implements View, IItemUpdatedListener {

    private static final long serialVersionUID = 1L;

    public AbstractFolderView(String title, FolderType folderType) {
        super(title, folderType);
    }

    @Override
    public void enter(ViewChangeEvent event) {
        refresh();
    }

}
