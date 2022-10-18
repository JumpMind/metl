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
import org.jumpmind.vaadin.ui.common.ResizableWindow;

import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.themes.ValoTheme;


@SuppressWarnings("serial")
public class ImagePreviewDialog extends ResizableWindow {
    
    public ImagePreviewDialog(Diagram diagram) {
        super("Image (right-click to save)");
        
        Panel panel = new Panel();
        panel.setId("canvasContainer");
        panel.setWidth("100%");  
        panel.setHeight("100%");
        addComponent(panel,1);
        
        Button closeButton = new Button("Close");
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        closeButton.addClickListener(new ClickListener() {
            @Override
            public void buttonClick(ClickEvent event) {
                close();
            }
        });
        
        // Create Footer Bar w/ download link.
        HorizontalLayout footer = new HorizontalLayout();
        footer.setWidth("100%");
        footer.setSpacing(true);
        footer.addStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        Label footerText = new Label("");
        footerText.setSizeUndefined();
        footer.addComponents(footerText);
        footer.setExpandRatio(footerText, 1);
        
        // Remove download link since it does not work with IE. May choose
        // to selectively show based on browser in the future.
//        Link downloadLink = new Link("Download", new ExternalResource("#"));
//        downloadLink.setId("downloadLink");
//        footer.addComponent(downloadLink);
        
        footer.addComponent(closeButton);
        this.addComponent(footer);
        
        diagram.export();
    }
  

}
