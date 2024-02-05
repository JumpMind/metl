package org.jumpmind.metl.ui.mapping;

import org.jumpmind.metl.core.model.Component;
import org.jumpmind.metl.core.model.HierarchicalModel;
import org.jumpmind.metl.core.model.RelationalModel;
import org.jumpmind.metl.core.runtime.component.Mapping;

public class MappingDiagramDetail {
    public String mapsToSchemaObjectName = Mapping.MODEL_OBJECT_MAPS_TO;
    public Component component;
    public RelationalModel relationalInputModel;
    public RelationalModel relationalOutputModel;
    public HierarchicalModel hierarchicalInputModel;
    public HierarchicalModel hierarchicalOutputModel;

    public Component getComponent() {
        return component;
    }

    public void setComponent(Component component) {
        this.component = component;
    }

    public RelationalModel getRelationalInputModel() {
        return relationalInputModel;
    }

    public void setRelationalInputModel(RelationalModel relationalInputModel) {
        this.relationalInputModel = relationalInputModel;
    }

    public RelationalModel getRelationalOutputModel() {
        return relationalOutputModel;
    }

    public void setRelationalOutputModel(RelationalModel relationalOutputModel) {
        this.relationalOutputModel = relationalOutputModel;
    }

    public HierarchicalModel getHierarchicalInputModel() {
        return hierarchicalInputModel;
    }

    public void setHierarchicalInputModel(HierarchicalModel hierarchicalInputModel) {
        this.hierarchicalInputModel = hierarchicalInputModel;
    }

    public HierarchicalModel getHierarchicalOutputModel() {
        return hierarchicalOutputModel;
    }

    public void setHierarchicalOutputModel(HierarchicalModel hierarchicalOutputModel) {
        this.hierarchicalOutputModel = hierarchicalOutputModel;
    }

}
