package org.jumpmind.metl.core.model;

abstract public class AbstractNamedObject extends AbstractObject {

    private static final long serialVersionUID = 1L;
    
    abstract public void setName(String name);
    
    abstract public String getName();
        
    @Override
    public String toString() {
        return getName();
    }
}
