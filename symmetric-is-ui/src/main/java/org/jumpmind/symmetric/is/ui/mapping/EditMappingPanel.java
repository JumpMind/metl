package org.jumpmind.symmetric.is.ui.mapping;

import org.jumpmind.symmetric.is.core.model.Component;
import org.jumpmind.symmetric.is.core.model.ComponentAttributeSetting;
import org.jumpmind.symmetric.is.core.model.ModelAttribute;
import org.jumpmind.symmetric.is.core.model.ModelEntity;
import org.jumpmind.symmetric.is.core.runtime.component.MappingProcessor;
import org.jumpmind.symmetric.is.ui.common.ApplicationContext;
import org.jumpmind.symmetric.is.ui.common.ButtonBar;
import org.jumpmind.symmetric.ui.common.IUiPanel;

import com.vaadin.server.FontAwesome;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.VerticalLayout;

@SuppressWarnings("serial")
public class EditMappingPanel extends VerticalLayout implements IUiPanel {

	ApplicationContext context;
	
	Component component;
	
	MappingDiagram diagram;

	public EditMappingPanel(ApplicationContext context, Component component) {
		this.context = context;
		this.component = component;
		
		ButtonBar buttonBar = new ButtonBar();
		addComponent(buttonBar);
		Button autoMapButton = buttonBar.addButton("Auto Map", FontAwesome.FLASH);
		Button removeButton = buttonBar.addButton("Remove", FontAwesome.TRASH_O);
		autoMapButton.addClickListener(new AutoMapListener());
		removeButton.addClickListener(new RemoveListener());

		diagram = new MappingDiagram(context, component);
		addComponent(diagram);
		setExpandRatio(diagram, 1.0f);		
	}

	class RemoveListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
		}
	}

	class AutoMapListener implements ClickListener {
		public void buttonClick(ClickEvent event) {
			for (ModelEntity entity : component.getInputModel().getModelEntities()) {
				for (ModelAttribute attr : entity.getModelAttributes()) {
					for (ModelEntity entity2 : component.getOutputModel().getModelEntities()) {
						for (ModelAttribute attr2 : entity2.getModelAttributes()) {
							boolean isMapped = false;
							for (ComponentAttributeSetting setting : component.getAttributeSettings()) {
								if (setting.getName().equals(MappingProcessor.ATTRIBUTE_MAPS_TO) &&
										setting.getValue().equals(attr2.getId())) {
									isMapped = true;
									break;
								}
							}
							if (!isMapped && fuzzyMatches(attr.getName(), attr2.getName())) {
								if (component.getAttributeSetting(attr.getId(), MappingProcessor.ATTRIBUTE_MAPS_TO) == null) {
									ComponentAttributeSetting setting = new ComponentAttributeSetting();
									setting.setAttributeId(attr.getId());
									setting.setComponentId(component.getId());
									setting.setName(MappingProcessor.ATTRIBUTE_MAPS_TO);
									setting.setValue(attr2.getId());
									component.addAttributeSetting(setting);
									context.getConfigurationService().save(setting);
									diagram.markAsDirty();
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public boolean closing() {
		return true;
	}

	@Override
	public void showing() {
	}

	boolean fuzzyMatches(String str1, String str2) {
		int x = computeLevenshteinDistance(str1, str2);
		return x < 3;
	}
	
	int computeLevenshteinDistance(CharSequence str1, CharSequence str2) {
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];

		for (int i = 0; i <= str1.length(); i++) {
			distance[i][0] = i;
		}
		for (int j = 1; j <= str2.length(); j++) {
			distance[0][j] = j;
		}

		for (int i = 1; i <= str1.length(); i++) {
			for (int j = 1; j <= str2.length(); j++) {
				distance[i][j] = minimum(distance[i - 1][j] + 1, distance[i][j - 1] + 1,
						distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));
			}
		}

		return distance[str1.length()][str2.length()];
	}
	
    int minimum(int a, int b, int c) {
        return Math.min(Math.min(a, b), c);
    }
}
