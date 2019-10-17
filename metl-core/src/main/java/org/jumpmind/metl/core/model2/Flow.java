package org.jumpmind.metl.core.model2;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@Data
@Builder
public class Flow {
    String id;
    String name;
    String notes;
    @Singular
    List<Step> steps;
    @Singular
    List<Parameter> parameters;
}
