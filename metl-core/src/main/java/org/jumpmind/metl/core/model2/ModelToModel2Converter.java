package org.jumpmind.metl.core.model2;

import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.model.FlowStep;
import org.jumpmind.metl.core.model.ProjectVersion;

import java.util.List;

public class ModelToModel2Converter {

    public static Config convert(ProjectVersion projectVersion, List<Flow> flows) {
        Config.ConfigBuilder configBuilder = Config.builder();
        for (Flow flow : flows) {
            configBuilder.flow(org.jumpmind.metl.core.model2.Flow.builder().
                    id(toCamelCase(flow.getName())).name(flow.getName()).notes(flow.getNotes()).build());
            List<FlowStep> flowStep = flow.getFlowSteps();
        }
        return configBuilder.build();
    }

    public static String toCamelCase(String value) {
        StringBuilder result = new StringBuilder();
        String[] words = value.split(" ");
        for (int i = 0; i < words.length; i++) {
            String word = words[i].trim();
            if (i == 0) {
                result.append(word.substring(0, 1).toLowerCase());
                result.append(word.substring(1, word.length()));
            } else {
                result.append(word.substring(0, 1).toUpperCase());
                result.append(word.substring(1, word.length()));
            }
        }
        return result.toString();
    }
}
