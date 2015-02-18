package org.jumpmind.symmetric.is.core.config;

import org.jumpmind.symmetric.is.core.config.data.ConnectionSettingData;
import org.jumpmind.symmetric.is.core.config.data.ModelFormatData;
import org.jumpmind.symmetric.is.core.config.data.SettingData;

public class Format extends AbstractObjectWithSettings<ModelFormatData> {

    private static final long serialVersionUID = 1L;

    public Format(ModelFormatData data) {
        this(null, data);
    }

    public Format(Folder folder, ModelFormatData data, SettingData... settings) {
        super(data, settings);
    }

    public String getType() {
    	return data.getType();
    }
    
    @Override
    protected SettingData createSettingData() {
        return new ConnectionSettingData(data.getId());
    }
    
    public void setName(String name) {
    }
    
    public String getName() {
        return this.data.getId();
    }
}
