package org.jumpmind.metl.ui.views;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.Serializable;

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
    
    HorizontalLayout importLayout;

    public ImportDialog(String caption, String text, final IImportListener importListener) {
        setCaption(caption);
        setModal(true);
        setResizable(true);
        setWidth(300, Unit.PIXELS);
        setHeight(200, Unit.PIXELS);
        setClosable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setSizeFull();
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);

        if (isNotBlank(text)) {
            Label textLabel = new Label(text);
            layout.addComponent(textLabel);
        }
        
        importLayout = new HorizontalLayout();
        importLayout.setWidth(100, Unit.PERCENTAGE);
        
        Upload upload;
        final UploadHandler handler = new UploadHandler();
        upload = new Upload(null, handler);
        upload.setImmediate(true);
        upload.setButtonCaption("Upload");
        upload.addFinishedListener(new Upload.FinishedListener() {
            private static final long serialVersionUID = 1L;

            public void uploadFinished(FinishedEvent event) {
                String content = handler.getContent();   
                importListener.onFinished(content);
                importLayout.removeAllComponents();
                Label label = new Label("Import Succeeded!");
                label.setStyleName(ValoTheme.LABEL_SUCCESS);
                importLayout.addComponent(label);
                importLayout.setComponentAlignment(label, Alignment.MIDDLE_CENTER);                
            }
        });
        importLayout.addComponent(upload);
        importLayout.setComponentAlignment(upload, Alignment.MIDDLE_CENTER);
        layout.addComponent(importLayout);

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setStyleName(ValoTheme.WINDOW_BOTTOM_TOOLBAR);
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth(100, Unit.PERCENTAGE);

        Label spacer = new Label(" ");
        buttonLayout.addComponent(spacer);
        buttonLayout.setExpandRatio(spacer, 1);

        Button cancelButton = new Button("Close");
        cancelButton.setClickShortcut(KeyCode.ESCAPE);
        cancelButton.addClickListener(new ClickListener() {
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(ClickEvent event) {
                UI.getCurrent().removeWindow(ImportDialog.this);
            }
        });
        buttonLayout.addComponent(cancelButton);
        
        layout.addComponent(buttonLayout);
        
        upload.focus();
        
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

