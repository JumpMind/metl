package org.jumpmind.symmetric.is.core.runtime.component;

@ComponentDefinition(category = ComponentCategory.PROCESSOR, typeName = "No Op", supports = {
        ComponentSupports.INPUT_MESSAGE, ComponentSupports.OUTPUT_MESSAGE })
public class NoOpProcessorComponent extends AbstractComponent {

}
