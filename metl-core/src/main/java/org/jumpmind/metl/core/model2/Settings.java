package org.jumpmind.metl.core.model2;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.Map;

@Builder
@Data
public class Settings {
    @Singular
    Map<String, String> settings;
}
