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
import org.jumpmind.vaadin.ui.common.ResizableDialog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.checkbox.CheckboxGroupVariant;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class TagDialog  extends ResizableDialog {

    final Logger log = LoggerFactory.getLogger(getClass());
    private static final long serialVersionUID = 1L;
    private IConfigurationService configurationService;
    private CheckboxGroup<Tag> tagGroup;
    private String entityId;
    private String entityType;
    
    public TagDialog(ApplicationContext context, Object selectedElement) {
        super("Tag Item");
        this.configurationService = context.getConfigurationService();
        initDialog(selectedElement);
    }
    
    private void initDialog(Object selectedItem) {
        H3 tagHeader = new H3("Select tags");
        VerticalLayout tagLayout = new VerticalLayout();
        tagLayout.setMargin(true);
        tagLayout.setSizeFull();
        addTagObjects(tagLayout, selectedItem);
        add(tagHeader, tagLayout, buildButtonFooter(buildCloseButton()));        
        setWidth("400px");
        setHeight("500px");
    }

    public void addTagObjects(VerticalLayout tagLayout, Object selected) {
        if (selected instanceof Project) {
            Project project = (Project) selected;
            entityId = project.getId();
            entityType = project.getClass().getName();
        }
        List<EntityTag> entityTags = configurationService.findEntityTagsForEntity(entityId);
        List<Tag> tags = configurationService.findTags();
        tagGroup = new CheckboxGroup<Tag>();
        tagGroup.setLabel("Tags");
        tagGroup.addThemeVariants(CheckboxGroupVariant.LUMO_VERTICAL);
        tagGroup.setItemLabelGenerator(tag -> tag.getName());
        tagGroup.setItems(tags);
        for (Tag tag : tags) {
            for (EntityTag entityTag:entityTags) {
                if (tag.getId().equals(entityTag.getTagId())) {
                    tagGroup.select(tag);
                    break;
                }
            }
        }
        tagGroup.addValueChangeListener(selectedItem -> updateAffectedObjects());      
        tagLayout.add(tagGroup);
    }

    private void updateAffectedObjects() {
        configurationService.deleteEntityTags(entityId);
        Set<Tag> tags = tagGroup.getValue();
        for (Tag tag : tags) {
            configurationService.save(new EntityTag(entityId, entityType, tag.getId()));
        }
    }

    public static void show(ApplicationContext context, Object selectedElement) {
        new TagDialog(context, selectedElement).open();
    }
    
}
