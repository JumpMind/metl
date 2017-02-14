package org.jumpmind.metl.core.persist;

import org.jumpmind.metl.core.model.AbstractObject;

public interface IConfigurationChangedListener {

    public void onSave(AbstractObject object);
    
    public void onDelete(AbstractObject object);
    
    public void onMultiRowUpdate();
    
}
