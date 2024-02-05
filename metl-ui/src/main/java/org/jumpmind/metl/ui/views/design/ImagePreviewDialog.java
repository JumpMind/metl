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

import org.jumpmind.metl.ui.diagram.Diagram;
import org.jumpmind.vaadin.ui.common.ResizableDialog;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;


@SuppressWarnings("serial")
public class ImagePreviewDialog extends ResizableDialog {
    
    public ImagePreviewDialog(Diagram diagram) {
        super("Image (right-click to save)");
        
        UI.getCurrent().getPage().retrieveExtendedClientDetails(details -> {
            setWidth((details.getWindowInnerWidth() * .75) + "px");
            setHeight((details.getWindowInnerHeight() * .75) + "px");
        });

        Div panel = new Div();
        panel.setId("canvasContainer");
        panel.setWidth("100%");  
        panel.setHeight("100%");
        addComponentAtIndex(1, panel);
        
        Button closeButton = new Button("Close");
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            @Override
            public void onComponentEvent(ClickEvent<Button> event) {
                close();
            }
        });
        
        // Create Footer Bar w/ download link.
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidth("100%");
        footer.setSpacing(true);
        Span footerText = new Span("");
        footerText.setSizeUndefined();
        footer.addAndExpand(footerText);
        
        // Remove download link since it does not work with IE. May choose
        // to selectively show based on browser in the future.
//        Link downloadLink = new Link("Download", new ExternalResource("#"));
//        downloadLink.setId("downloadLink");
//        footer.addComponent(downloadLink);
        
        footer.add(closeButton);
        this.add(footer);
        
        diagram.export();
    }
  

}
