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
package org.jumpmind.metl.ui.common;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

import org.jumpmind.metl.core.util.MessageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ProgressBar;
import com.vaadin.ui.UI;
import com.vaadin.ui.Upload;
import com.vaadin.ui.Upload.FinishedEvent;
import com.vaadin.ui.Upload.Receiver;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;
import com.vaadin.ui.themes.ValoTheme;

public class ImportDialog extends Window {

    private static final long serialVersionUID = 1L;
    
    final protected Logger log = LoggerFactory.getLogger(getClass()); 
    
    HorizontalLayout importLayout;
    
    IImportListener importListener;
    
    Upload upload;
    
    HorizontalLayout buttonLayout;

    public ImportDialog(String caption, String text, IImportListener importListener) {
        this.importListener = importListener;
        
        setCaption(caption);
        setModal(true);
        setResizable(true);
        setWidth(305, Unit.PIXELS);
        setClosable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setWidth(100, Unit.PERCENTAGE);
        layout.setMargin(true);
        layout.setSpacing(true);
        setContent(layout);

        if (isNotBlank(text)) {
            Label textLabel = new Label(text);
            layout.addComponent(textLabel);
        }
        
        importLayout = new HorizontalLayout();
        importLayout.setWidth(100, Unit.PERCENTAGE);
        
        layout.addComponent(importLayout);
        layout.setExpandRatio(importLayout, 1);

        buttonLayout = new HorizontalLayout();
        buttonLayout.setStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth(100, Unit.PERCENTAGE);

        Label spacer = new Label(" ");
        buttonLayout.addComponent(spacer);
        buttonLayout.setExpandRatio(spacer, 1);       

        Button closeButton = new Button("Close");
        closeButton.addStyleName(ValoTheme.BUTTON_PRIMARY);
        closeButton.setClickShortcut(KeyCode.ESCAPE);
        closeButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                UI.getCurrent().removeWindow(ImportDialog.this);
            }
        });
        buttonLayout.addComponent(closeButton);
        
        layout.addComponent(buttonLayout);
        
        replaceUploadButton();
        
        
    }
    
    protected void replaceUploadButton() {
        if (upload != null) {
            buttonLayout.removeComponent(upload);
        }
        final UploadHandler handler = new UploadHandler();
        upload = new Upload(null, handler);
        upload.setImmediate(true);
        upload.setButtonCaption("Upload");
        upload.addFinishedListener(new Upload.FinishedListener() {
            private static final long serialVersionUID = 1L;

            public void uploadFinished(FinishedEvent event) {
                try {
                    String content = handler.getContent();   
                    importListener.onFinished(content);
                    importLayout.removeAllComponents();
                    Label label = new Label("Import Succeeded!");
                    label.setStyleName(ValoTheme.LABEL_SUCCESS);
                    importLayout.addComponent(label);
                    importLayout.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
                } catch (Exception e) {
                    log.error("Import failed", e);
                    importLayout.removeAllComponents();
                    String message = "Import Failed! Please check log file for details.";
                    if (e instanceof MessageException) {
                        message = e.getMessage();
                    }
                    Label label = new Label(message);
                    label.setStyleName(ValoTheme.LABEL_FAILURE);
                    importLayout.addComponent(label);
                    importLayout.setComponentAlignment(label, Alignment.MIDDLE_CENTER);
                }             
                replaceUploadButton();
            }
        });

        buttonLayout.addComponent(upload, 1);
    }

    public static void show(String caption, String text, IImportListener listener) {
        ImportDialog dialog = new ImportDialog(caption, text, listener);
        UI.getCurrent().addWindow(dialog);
    }

    public static interface IImportListener extends Serializable {
        public void onFinished(String dataToImport);
    }

    class UploadHandler implements Receiver {

        private static final long serialVersionUID = 1L;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        @Override
        public OutputStream receiveUpload(String filename, String mimeType) {
            importLayout.removeAllComponents();
            ProgressBar bar = new ProgressBar();
            bar.setIndeterminate(true);
            importLayout.addComponent(bar);
            importLayout.setComponentAlignment(bar, Alignment.MIDDLE_CENTER);
            return os;
        }       

        public void reset() {
            os = new ByteArrayOutputStream();
        }

        public String getContent() {
            return os.toString();
        }

    }
    
}

