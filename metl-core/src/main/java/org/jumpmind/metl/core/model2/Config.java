package org.jumpmind.metl.core.model2;

import groovy.lang.Singleton;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.ArrayList;
import java.util.List;

@Builder
@Data
public class Config {
    List<String> imports;
    @Singular
    List<Flow> flows;
    List<Model> models;
    List<Resource> resources;

}
