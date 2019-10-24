package org.jumpmind.metl.core.model2;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.jumpmind.metl.core.model.Setting;

import java.util.List;
import java.util.Map;

@Builder
@Data
public class ModelSettings {
    @Singular
    Map<String, EntitySettings> entitySettings;
}
