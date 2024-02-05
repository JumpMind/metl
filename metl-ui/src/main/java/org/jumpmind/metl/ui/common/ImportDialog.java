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

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.upload.FinishedEvent;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.Upload;

public class ImportDialog extends Dialog {

    private static final long serialVersionUID = 1L;
    
    final protected Logger log = LoggerFactory.getLogger(getClass()); 
    
    HorizontalLayout importLayout;
    
    IImportListener importListener;
    
    Upload upload;
    
    HorizontalLayout buttonLayout;

    public ImportDialog(String caption, String text, IImportListener importListener) {
        this.importListener = importListener;
        
        setModal(true);
        setResizable(true);
        setWidth("305px");

        Span header = new Span("<b>" + caption + "</b><hr>");
        header.setWidthFull();
        header.getStyle().set("margin", null);
        add(header);

        VerticalLayout layout = new VerticalLayout();
        layout.setWidthFull();
        layout.setMargin(true);
        layout.setSpacing(true);
        add(layout);

        if (isNotBlank(text)) {
            Span textSpan = new Span(text);
            layout.add(textSpan);
        }
        
        importLayout = new HorizontalLayout();
        importLayout.setWidthFull();
        
        layout.add(importLayout);
        layout.expand(importLayout);

        buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidthFull();

        Span spacer = new Span(" ");
        buttonLayout.addAndExpand(spacer);    

        Button closeButton = new Button("Close");
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addClickShortcut(Key.ESCAPE);
        closeButton.addClickListener(new ComponentEventListener<ClickEvent<Button>>() {
            private static final long serialVersionUID = 1L;

            @Override
            public void onComponentEvent(ClickEvent<Button> event) {
                close();
            }
        });
        buttonLayout.add(closeButton);
        
        layout.add(buttonLayout);
        
        replaceUploadButton();
        
        
    }
    
    protected void replaceUploadButton() {
        if (upload != null) {
            buttonLayout.remove(upload);
        }
        final UploadHandler handler = new UploadHandler();
        upload = new Upload(handler);
        upload.addFinishedListener(new ComponentEventListener<FinishedEvent>() {
            private static final long serialVersionUID = 1L;

            public void onComponentEvent(FinishedEvent event) {
                try {
                    String content = handler.getContent();   
                    importListener.onFinished(content);
                    importLayout.removeAll();
                    Span span = new Span("Import Succeeded!");
                    //span.setStyleName(ValoTheme.LABEL_SUCCESS);
                    importLayout.add(span);
                    importLayout.setVerticalComponentAlignment(Alignment.CENTER, span);
                } catch (Exception e) {
                    log.error("Import failed", e);
                    importLayout.removeAll();
                    String message = "Import Failed! Please check log file for details.";
                    if (e instanceof MessageException) {
                        message = e.getMessage();
                    }
                    Span span = new Span(message);
                    //span.setStyleName(ValoTheme.LABEL_FAILURE);
                    importLayout.add(span);
                    importLayout.setVerticalComponentAlignment(Alignment.CENTER, span);
                }             
                replaceUploadButton();
            }
        });

        buttonLayout.addComponentAtIndex(1, upload);
    }

    public static void show(String caption, String text, IImportListener listener) {
        new ImportDialog(caption, text, listener).open();
    }

    public static interface IImportListener extends Serializable {
        public void onFinished(String dataToImport);
    }

    class UploadHandler implements Receiver {

        private static final long serialVersionUID = 1L;
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        @Override
        public OutputStream receiveUpload(String filename, String mimeType) {
            importLayout.removeAll();
            ProgressBar bar = new ProgressBar();
            bar.setIndeterminate(true);
            importLayout.add(bar);
            importLayout.setVerticalComponentAlignment(Alignment.CENTER, bar);
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

