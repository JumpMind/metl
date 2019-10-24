package org.jumpmind.metl.core.model2;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;
import java.util.Map;

@Builder
@Data
public class Step {
    String id;
    String name;
    int x;
    int y;
    String type;
    @Singular
    Map<String, String> resources;
    String inputModelId;
    String outputModelId;
    @Singular
    Map<String, String> settings;
    @Singular
    Map<String, ModelSettings> modelSettings;
    @Singular
    Map<String, List<String>> mappings;

}
