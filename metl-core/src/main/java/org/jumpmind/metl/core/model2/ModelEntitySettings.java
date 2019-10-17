package org.jumpmind.metl.core.model2;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ModelEntitySettings {
    String entityId;
    List<Setting> settings;
    Map<String, List<Setting>> attributeSettings;
}
