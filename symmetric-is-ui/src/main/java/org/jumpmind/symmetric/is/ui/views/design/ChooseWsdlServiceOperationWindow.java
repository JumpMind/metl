package org.jumpmind.symmetric.is.ui.views.design;

import java.io.Serializable;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.reficio.ws.builder.SoapOperation;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

@SuppressWarnings("serial")
public class ChooseWsdlServiceOperationWindow extends Window {

    public ChooseWsdlServiceOperationWindow(List<SoapOperation> operations,
            final ServiceChosenListener listener) {
        setCaption("Choose SOAP Operation");
        setModal(true);
        setResizable(false);
        setSizeUndefined();
        setClosable(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        setContent(layout);

        layout.addComponent(new Label("Choose the SOAP operation to use."));

        final ListSelect field = new ListSelect();
        field.setNullSelectionAllowed(false);
        field.setMultiSelect(false);
        field.setWidth(100, Unit.PERCENTAGE);
        field.setRows(15);
        layout.addComponent(field);

        Collections.sort(operations, new Comparator<SoapOperation>() {
            public int compare(SoapOperation o1, SoapOperation o2) {
                return o1.getOperationName().compareTo(o2.getOperationName());
            }
        });

        for (SoapOperation operation : operations) {
            field.addItem(operation);            
            field.setItemCaption(operation, operation.getBindingName().getLocalPart() + "." + operation.getOperationName());
        }

        HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        buttonLayout.setWidth(100, Unit.PERCENTAGE);

        Label spacer = new Label(" ");
        buttonLayout.addComponent(spacer);
        buttonLayout.setExpandRatio(spacer, 1);

        Button cancelButton = new Button("Cancel");
        cancelButton.setClickShortcut(KeyCode.ESCAPE);
        cancelButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                UI.getCurrent().removeWindow(ChooseWsdlServiceOperationWindow.this);                
            }
        });
        buttonLayout.addComponent(cancelButton);

        Button okButton = new Button("Ok");
        okButton.setClickShortcut(KeyCode.ENTER);
        okButton.addClickListener(new ClickListener() {
            public void buttonClick(ClickEvent event) {
                if (listener.onOk((SoapOperation) field.getValue())) {
                    UI.getCurrent().removeWindow(ChooseWsdlServiceOperationWindow.this);
                }
            }
        });
        buttonLayout.addComponent(okButton);

        layout.addComponent(buttonLayout);
        field.focus();
    }

    public static interface ServiceChosenListener extends Serializable {
        public boolean onOk(SoapOperation operation);
    }

}