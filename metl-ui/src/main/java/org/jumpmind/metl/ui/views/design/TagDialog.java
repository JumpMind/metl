package org.jumpmind.metl.ui.views.design;

import java.util.List;
import java.util.Set;

import org.jumpmind.metl.core.model.EntityTag;
import org.jumpmind.metl.core.model.Project;
import org.jumpmind.metl.core.model.Tag;
import org.jumpmind.metl.core.persist.IConfigurationService;
import org.jumpmind.metl.ui.common.ApplicationContext;
import org.jumpmind.vaadin.ui.common.ResizableWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

public class TagDialog  extends ResizableWindow {

    final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1L;
    private IConfigurationService configurationService;
    private OptionGroup tagGroup;
    private String entityId;
    private String entityType;
    
    public TagDialog(ApplicationContext context, Object selectedElement) {
        super("Tag Item");
        this.configurationService = context.getConfigurationService();
        initWindow(selectedElement);
    }
    
    private void initWindow(Object selectedItem) {
        Panel tagPanel = new Panel("Select tags");
        tagPanel.addStyleName(ValoTheme.PANEL_SCROLL_INDICATOR);
        tagPanel.setSizeFull();
        VerticalLayout tagLayout = new VerticalLayout();
        tagLayout.setMargin(true);
        addTagObjects(tagLayout, selectedItem);
        tagPanel.setContent(tagLayout);
        addComponent(tagPanel, 1);        
        addComponent(buildButtonFooter(buildCloseButton()));
        setWidth(400, Unit.PIXELS);
        setHeight(500, Unit.PIXELS);
    }

    public void addTagObjects(VerticalLayout tagLayout, Object selected) {
        if (selected instanceof Project) {
            Project project = (Project) selected;
            entityId = project.getId();
            entityType = project.getClass().getName();
        }
        List<EntityTag> entityTags = configurationService.findEntityTagsForEntity(entityId);
        List<Tag> tags = configurationService.findTags();
        tagGroup = new OptionGroup("Tags");
        tagGroup.addStyleName(ValoTheme.OPTIONGROUP_SMALL);
        tagGroup.setMultiSelect(true);
        for (Tag tag : tags) {
            tagGroup.addItem(tag.getId());
            tagGroup.setItemCaption(tag.getId(), tag.getName());
            for (EntityTag entityTag:entityTags) {
                if (tag.getId().equals(entityTag.getTagId())) {
                    tagGroup.select(tag.getId());
                    break;
                }
            }
        }
        tagGroup.addValueChangeListener(selectedItem -> updateAffectedObjects());      
        tagLayout.addComponent(tagGroup);
    }

    @SuppressWarnings("unchecked")
    private void updateAffectedObjects() {
        configurationService.deleteEntityTags(entityId);
        Set<String> tagIds = (Set<String>) tagGroup.getValue();
        for (String tagId : tagIds) {
            configurationService.save(new EntityTag(entityId, entityType, tagId));
        }
    }

    public static void show(ApplicationContext context, Object selectedElement) {
        TagDialog dialog = new TagDialog(context, selectedElement);
        UI.getCurrent().addWindow(dialog);
    }
    
}
