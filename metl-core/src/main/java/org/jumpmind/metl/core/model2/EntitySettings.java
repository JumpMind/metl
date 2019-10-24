package org.jumpmind.metl.core.model2;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;

@Builder
@Data
public class EntitySettings {
    String entityId;
    @Singular
    Map<String, String> entitySettings;
    @Singular
    Map<String, Settings> attributeSettings;
}
