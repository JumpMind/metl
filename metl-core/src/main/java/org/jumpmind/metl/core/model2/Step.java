package org.jumpmind.metl.core.model2;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class Step {
    String id;
    String name;
    boolean enabled;
    int x;
    int y;
    String type;
    Map<String, String> resources;
    String inputModelId;
    String outputModelId;
    List<Setting> settings;

}
