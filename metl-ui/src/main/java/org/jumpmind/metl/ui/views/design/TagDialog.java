/**
 * Licensed to JumpMind Inc under one or more contributor
 * license agreements.  See the NOTICE file distributed
 * with this work for additional information regarding
 * copyright ownership.  JumpMind Inc licenses this file
 * to you under the GNU General Public License, version 3.0 (GPLv3)
 * (the "License"); you may not use this file except in compliance
 * with the License.
 *
 * You should have received a copy of the GNU General Public License,
 * version 3.0 (GPLv3) along with this library; if not, see
 * <http://www.gnu.org/licenses/>.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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

import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.themes.ValoTheme;
import com.vaadin.v7.ui.OptionGroup;
import com.vaadin.v7.ui.VerticalLayout;

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
