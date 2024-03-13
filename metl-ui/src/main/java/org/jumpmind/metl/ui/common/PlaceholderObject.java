package org.jumpmind.metl.ui.common;

import org.jumpmind.metl.core.model.AbstractNamedObject;

public class PlaceholderObject extends AbstractNamedObject {
    private static final long serialVersionUID = 1L;

    public PlaceholderObject(String parentId) {
        setId("child of " + parentId);
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public String getName() {
        return "<empty>";
    }
}