package org.jumpmind.metl.core.model2;

import org.jumpmind.metl.core.model.*;
import org.jumpmind.metl.core.model.Flow;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.core.plugin.IDefinitionFactory;
import org.jumpmind.metl.core.plugin.XMLComponentDefinition;
import org.jumpmind.metl.core.runtime.component.IComponentRuntimeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static org.apache.commons.lang.StringUtils.*;

public class ModelToYamlConverter {

    @Autowired
    IConfigurationService configurationService;

    @Autowired
    IDefinitionFactory definitionFactory;

    public Config convert(String projectVersionId, List<Flow> flows) {
        Config.ConfigBuilder configBuilder = Config.builder();
        flows.forEach((flow) -> {
            org.jumpmind.metl.core.model2.Flow.FlowBuilder flowBuilder = org.jumpmind.metl.core.model2.Flow.builder().
                    id(toCamelCase(flow.getName())).name(flow.getName()).notes(flow.getNotes());
            flow.getFlowSteps().forEach((step) -> {
                Component comp = step.getComponent();
                XMLComponentDefinition definition = definitionFactory.getComponentDefinition(projectVersionId, comp.getType());
                Step.StepBuilder stepBuilder = Step.builder().x(step.getX()).y(step.getY()).type(toCamelCase(definition.getName())).name(comp.getName()).id(toCamelCase(comp.getName()));
                comp.getSettings().forEach((setting) -> {
                    stepBuilder.setting(setting.getName(), setting.getValue());
                });

                convertMapping(stepBuilder, comp);
                convertModelSettings(isNotBlank(comp.getInputModelId()) ? configurationService.findRelationalModel(comp.getInputModelId()) : null, comp, stepBuilder);
                convertModelSettings(isNotBlank(comp.getOutputModelId()) ? configurationService.findRelationalModel(comp.getOutputModelId()) : null, comp, stepBuilder);

                flowBuilder.step(stepBuilder.build());
            });
            configBuilder.flow(flowBuilder.build());
        });
        return configBuilder.build();
    }

    protected void convertModelSettings(RelationalModel model, Component comp, Step.StepBuilder stepBuilder) {
        if (model != null) {
            ModelSettings.ModelSettingsBuilder modelSettingsBuilder = ModelSettings.builder();
            Map<String, EntitySettings.EntitySettingsBuilder> entitySettingsBuilders = new HashMap<>();
            comp.getEntitySettings().stream().filter((e) -> model.getEntityById(e.getEntityId()) != null).forEach(
                    (e) -> {
                        EntitySettings.EntitySettingsBuilder builder = entitySettingsBuilders.get(e.getEntityId());
                        if (builder == null) {
                            builder = EntitySettings.builder().entityId(e.getName());
                            entitySettingsBuilders.put(e.getEntityId(), builder);
                        }
                        builder.entitySetting(e.getName(), e.getValue());
                    }
            );

            Map<String, Settings.SettingsBuilder> attributeSettingBuilders = new HashMap<>();
            comp.getAttributeSettings().forEach((a)-> {
                ModelAttrib attrib = model.getAttributeById(a.getAttributeId());
                if (attrib != null) {
                    EntitySettings.EntitySettingsBuilder builder = entitySettingsBuilders.get(attrib.getEntityId());
                    if (builder == null) {
                        builder = EntitySettings.builder().entityId(model.getEntityById(attrib.getEntityId()).getName());
                        entitySettingsBuilders.put(attrib.getEntityId(), builder);
                    }

                    Settings.SettingsBuilder attributeBuilder = attributeSettingBuilders.get(attrib.getId());
                    if (attributeBuilder == null) {
                        attributeBuilder = Settings.builder();
                        attributeSettingBuilders.put(attrib.getId(), attributeBuilder);
                    }

                    attributeBuilder.setting(a.getName(), a.getValue());
                }
            });

            entitySettingsBuilders.keySet().forEach((entityId)-> {
                ModelEntity entity = model.getEntityById(entityId);
                EntitySettings.EntitySettingsBuilder entitySettingsBuilder = entitySettingsBuilders.get(entityId);
                entity.getModelAttributes().forEach((attrib) -> {
                    if (attributeSettingBuilders.containsKey(attrib.getId())) {
                        entitySettingsBuilder.attributeSetting(attrib.getName(), attributeSettingBuilders.get(attrib.getId()).build());
                    }
                });
                modelSettingsBuilder.entitySetting(entity.getName(), entitySettingsBuilder.build());
            });

            stepBuilder.modelSetting(toCamelCase(model.getName()), modelSettingsBuilder.build());

        }

    }

    protected void convertMapping(Step.StepBuilder stepBuilder, Component comp) {
        Map<String, List<ComponentModelSetting>> mapBySourceAttributeId = comp.getModelSettings().stream().collect(Collectors.groupingBy(ComponentModelSetting::getModelObjectId));
        if (mapBySourceAttributeId.size() > 0) {
            RelationalModel inputModel = configurationService.findRelationalModel(comp.getInputModelId());
            RelationalModel outputModel = configurationService.findRelationalModel(comp.getOutputModelId());
            mapBySourceAttributeId.keySet().forEach((sourceAttributeId) -> {
                stepBuilder.mapping(fullyQualifiedAttribName(inputModel, sourceAttributeId),
                        mapBySourceAttributeId.get(sourceAttributeId)
                                .stream().map((setting)->{
                            return fullyQualifiedAttribName(outputModel, setting.getValue());
                        }).collect(Collectors.toList()));
            });
        }
    }

    protected String fullyQualifiedAttribName(RelationalModel model, String attributeId) {
        ModelAttrib attrib = model.getAttributeById(attributeId);
        return new StringBuilder(toCamelCase(model.getName())).append(".").append(
                model.getEntityById(attrib.getEntityId()).getName()).append(".").append(attrib.getName()).toString();
    }

    public String toYaml(String projectVersionId, List<Flow> flows) {
        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                // if value of property is null, ignore it.
                if (propertyValue == null || (propertyValue instanceof Collection && ((Collection) propertyValue).size() == 0) ||
                        (propertyValue instanceof Map && ((Map) propertyValue).size() == 0)) {
                    return null;
                } else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }

            @Override
            protected Node representScalar(Tag tag, String value, DumperOptions.ScalarStyle style) {
                style = value != null &&
                        ((value.contains("<") && (value.contains("/>") || value.contains("</"))) ||
                                value.length() > 255 ||
                                value.contains("\n")) ? DumperOptions.ScalarStyle.LITERAL : DumperOptions.ScalarStyle.SINGLE_QUOTED;
                return super.representScalar(tag, value, style);
            }
        };
        representer.addClassTag(Config.class, new Tag("!config"));
        DumperOptions options = new DumperOptions();
        options.setPrettyFlow(true);
        options.setIndent(4);
        return new Yaml(representer, options).dump(convert(projectVersionId, flows));

    }

    protected String toCamelCase(String value) {
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
